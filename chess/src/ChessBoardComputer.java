/**
 * ChessBoardComputer.java
 * 
 * Divyam Banga
 * ICS4U, Mr. Mckenzie
 * 
 * Extends from chess board and builds on it by adding an AI opponet that calculates moves using minimax algorithm and
 * alpha beta pruning to improve efficiency. Currently set to calculate for 5 seconds before making a move, searches as many 
 * depths as it can in 5 seconds and makes the best move found.
 * 
 */


import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;


public class ChessBoardComputer extends ChessBoard {
    public boolean isComputerWhite;
    private static final long TIME_LIMIT = 5000; //5 seconds in miliseconds
    private long startTime;
    private boolean timeUp;
    private Move bestMoveSoFar;//track best move found 
    public int count = 0;
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);//UI stuff(found on stack overflow)
    private int lastSearchDepth = 0;

    //variables to highlight compouters moves
    private Point lastMoveFrom;
    private Point lastMoveTo;
    private static final Color MOVE_FROM_COLOR = new Color(255, 255, 0, 100);
    private static final Color MOVE_TO_COLOR = new Color(0, 255, 0, 100);
    

    //contructor to set up
    public ChessBoardComputer(boolean playAsWhite) {
        super();
        this.isComputerWhite = !playAsWhite;
    }

    //UI stuff found online
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    //getter
    public int getLastSearchDepth() {
        return lastSearchDepth;
    }

    //for move highlighting
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        //draw move indicators if they exist
        if (lastMoveFrom != null && lastMoveTo != null) {
            Graphics2D g2d = (Graphics2D) g;
            int tileSize = getTileSize();

            //draw source square highlight
            g2d.setColor(MOVE_FROM_COLOR);
            g2d.fillRect(lastMoveFrom.x * tileSize, lastMoveFrom.y * tileSize, tileSize, tileSize);

            //draw destination square highlight
            g2d.setColor(MOVE_TO_COLOR);
            g2d.fillRect(lastMoveTo.x * tileSize, lastMoveTo.y * tileSize, tileSize, tileSize);
        }
    }

    //computer always chooses queen to promote to
    @Override
    public String handlePawnPromotion(int row, int col, boolean isWhite) {
        if (isWhite == isComputerWhite) {
            return isWhite ? "queen" : "queen1";
        }
        return super.handlePawnPromotion(row, col, isWhite);
    }


    @Override
    void handleMouseClick(Point click) {
        //ignore clicks during computers turn
        if (isWhiteTurn() == isComputerWhite) {
            return;
        }
        boolean initialTurn = isWhiteTurn();
        
        //clear previous move highlights when starting a new move
        if (getSelectedPiece() == null) {
            lastMoveFrom = null;
            lastMoveTo = null;
            repaint();
        }
        
        super.handleMouseClick(click);
        
        //if turn changed to computer turn, start computer move
        if (initialTurn != isWhiteTurn() && isWhiteTurn() == isComputerWhite) {
            repaint();
            pcs.firePropertyChange("computerThinking", false, true);
    
            //run computer move in seperate thread so doesn't freeze/ lags (Used some AI to help with this and Ario)
            new Thread(() -> {
                makeComputerMove();
                SwingUtilities.invokeLater(() -> pcs.firePropertyChange("computerThinking", true, false));
            }).start();
        }
        count = 0;
    }
    
    private void makeComputerMove() {
        Move bestMove = findBestMove();
        if (bestMove != null) {
            //store the move coordinates for highlighting
            lastMoveFrom = new Point(bestMove.fromCol, bestMove.fromRow);
            lastMoveTo = new Point(bestMove.toCol, bestMove.toRow);

            //simulate computer move as clicks
            SwingUtilities.invokeLater(() -> {
                Point originalClick = new Point(bestMove.fromCol * getTileSize() + getTileSize() / 2,
                        bestMove.fromRow * getTileSize() + getTileSize() / 2);
                super.handleMouseClick(originalClick);

                Point targetClick = new Point(bestMove.toCol * getTileSize() + getTileSize() / 2,
                        bestMove.toRow * getTileSize() + getTileSize() / 2);
                super.handleMouseClick(targetClick);
                
                repaint();
            });
        }
    }

    //Iterative deepening to find best move in 5 seconds
    private Move findBestMove() {
        startTime = System.currentTimeMillis();
        timeUp = false;
        bestMoveSoFar = null;
        int depth = 1;

        //keep searching until time runs out
        while (!timeUp) {
            System.out.println("Searching depth: " + depth);
            Move move = searchAtDepth(depth);

            if (!timeUp) {
                bestMoveSoFar = move;
                lastSearchDepth = depth;
                System.out.println("Completed depth " + depth + " search");
                depth++;
            }

            if (System.currentTimeMillis() - startTime >= TIME_LIMIT) {
                timeUp = true;
                System.out.println("Time up! Reached depth: " + (depth - 1));
            }
        }

        //convert to move notation to display on side window
        if (bestMoveSoFar != null) {
            String moveNotation = String.format("%c%d to %c%d",
                    (char) ('a' + bestMoveSoFar.fromCol), 8 - bestMoveSoFar.fromRow,
                    (char) ('a' + bestMoveSoFar.toCol), 8 - bestMoveSoFar.toRow);
            pcs.firePropertyChange("moveMade", null, moveNotation);
        }

        return bestMoveSoFar;
    }

    //searches at a specific depth using minimax algorithm
    private Move searchAtDepth(int depth) {
        Move bestMove = null;
        int bestValue = Integer.MIN_VALUE;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;

        //make board copy to change
        String[][] boardCopy = copyBoard();
        List<Move> possibleMoves = getAllPossibleMoves(isComputerWhite);

        
        for (Move move : possibleMoves) {
            //if out of time return the best move found
            if (timeUp) {
                return bestMoveSoFar;
            }
            makeMove(move);//simulate move
            if (checkmate(!isComputerWhite)) {
                undoMove(move, boardCopy);
                return move; //immediate checkmate
            }

            //use recursion to evaluate position
            int value = minimax(depth - 1, false, alpha, beta);
            undoMove(move, boardCopy);

            //check for time again
            if (timeUp) {
                return bestMoveSoFar;
            }

            //update best move if betetr
            if (value > bestValue) {
                bestValue = value;
                bestMove = move;
            }
            //update alpha for pruning
            alpha = Math.max(alpha, bestValue);

            //final time check before moving in
            if (System.currentTimeMillis() - startTime >= TIME_LIMIT) {
                timeUp = true;
                return bestMoveSoFar;
            }
        }
        return bestMove;
    }

    //recursively evaluates positions and evaluates them using evaluate function
    private int minimax(int depth, boolean isMaximizing, int alpha, int beta) {
        //check for time
        if (System.currentTimeMillis() - startTime >= TIME_LIMIT) {
            timeUp = true;
            return evaluatePosition();
        }

        //base case
        if (depth == 0) {
            count++;
            return evaluatePosition();
        }

        List<Move> moves = getAllPossibleMoves(isMaximizing ? isComputerWhite : !isComputerWhite);

        if (moves.isEmpty()) {
            if (checkmate(isMaximizing ? isComputerWhite : !isComputerWhite)) {
                return isMaximizing ? -10000 : 10000;
            }
            return 0; //stalemate
        }

        String[][] boardCopy = copyBoard();

        if (isMaximizing) {
            int maxEval = Integer.MIN_VALUE;
            for (Move move : moves) {
                if (timeUp) return maxEval;

                makeMove(move);
                int eval = minimax(depth - 1, false, alpha, beta);//recursively evaluate position after the move
                undoMove(move, boardCopy);

                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                //if we have a better path, eliminate all possibilies or like series of moves
                if (beta <= alpha) {
                    break;
                }
            }
            return maxEval;
        } else {
            //next turn
            int minEval = Integer.MAX_VALUE;
            for (Move move : moves) {
                if (timeUp) return minEval;

                makeMove(move);
                int eval = minimax(depth - 1, true, alpha, beta);//recursively evaluate position after the move
                undoMove(move, boardCopy);

                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                //make sure this path wont be chosen
                if (beta <= alpha) {
                    break;
                }
            }
            return minEval;
        }
    }

    //gets all possible moves at a given position
    private List<Move> getAllPossibleMoves(boolean forWhite) {
        List<Move> allMoves = new ArrayList<>();
        for (int row = 0; row < getBoardSize(); row++) {
            for (int col = 0; col < getBoardSize(); col++) {
                String piece = getPieceLocations()[row][col];
                if (piece != null && (!piece.endsWith("1") == forWhite)) {
                    List<Point> validMoves = generateValidMoves(row, col);
                    for (Point move : validMoves) {
                        allMoves.add(new Move(row, col, move.y, move.x));
                    }
                }
            }
        }
        return allMoves;
    }

    //evaluates a position using piece positions, and piece values
    private int evaluatePosition() {
        int totalScore = 0;
        String[][] board = getPieceLocations();

        //basic piece values
        Map<String, Integer> pieceValues = new HashMap<>();
        pieceValues.put("pawn", 100);
        pieceValues.put("knight", 320);
        pieceValues.put("bishop", 330);
        pieceValues.put("rook", 500);
        pieceValues.put("queen", 900);
        pieceValues.put("king", 20000);

        //gets position bonus for all the pieces
        for (int row = 0; row < getBoardSize(); row++) {
            for (int col = 0; col < getBoardSize(); col++) {
                String piece = board[row][col];
                if (piece != null) {
                    String basePiece = piece.replace("1", "");
                    int value = pieceValues.get(basePiece);
                    value += getPositionalBonus(basePiece, row, col, piece.endsWith("1"));
                    if (!piece.endsWith("1") == isComputerWhite) {
                        totalScore += value;
                    } else {
                        totalScore -= value;
                    }
                }
            }
        }

        return totalScore;
    }

    //returns bonus points depending in position of pieces
    private int getPositionalBonus(String piece, int row, int col, boolean isBlack) {
        if (isBlack) {
            row = 7 - row;
        }
        switch (piece) {
            case "pawn":
                return 10 * (7 - row);//pawns worth more as they advance
            case "knight":
                return (int) ((Math.abs(col - 3.5) + Math.abs(row - 3.5)) * -5);//knights better in center
            case "bishop":
                return (int) ((Math.abs(col - 3.5) + Math.abs(row - 3.5)) * -3);//bishops better in center
            case "rook":
                return col == 3 || col == 4 ? 10 : 0;//rooks better in central columns
            case "queen":
                return (int) ((Math.abs(col - 3.5) + Math.abs(row - 3.5)) * -2);//queen slightly better in center
            case "king":
                if (row < 2) {
                    return 20;//king better in the back
                }
                return 0;
            default:
                return 0;
        }
    }

    //helper method to make move
    private void makeMove(Move move) {
        String piece = getPieceLocations()[move.fromRow][move.fromCol];
        getPieceLocations()[move.toRow][move.toCol] = piece;
        getPieceLocations()[move.fromRow][move.fromCol] = null;
    }

    //helper method to undow move
    private void undoMove(Move move, String[][] originalBoard) {
        for (int i = 0; i < getBoardSize(); i++) {
            System.arraycopy(originalBoard[i], 0, getPieceLocations()[i], 0, getBoardSize());
        }
    }

    //helper method to create a copy of the board
    private String[][] copyBoard() {
        String[][] copy = new String[getBoardSize()][getBoardSize()];
        for (int i = 0; i < getBoardSize(); i++) {
            System.arraycopy(getPieceLocations()[i], 0, copy[i], 0, getBoardSize());
        }
        return copy;
    }

    //helper class to represent move
    private static class Move {
        int fromRow, fromCol, toRow, toCol;

        public Move(int fromRow, int fromCol, int toRow, int toCol) {
            this.fromRow = fromRow;
            this.fromCol = fromCol;
            this.toRow = toRow;
            this.toCol = toCol;
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("KDestroyer");//name after destroying kevin dang hence KDestroyer
        ChessBoardComputer board = new ChessBoardComputer(true);

        frame.add(board);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}