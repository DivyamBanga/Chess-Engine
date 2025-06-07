/**
 * ChessBoard.java
 * 
 * Divyam Banga
 * ICS4U, Mr. Mckenzie
 * 
 * Base chess game includes move generation for all pieces, castling, en pasant, ckekmate, stalemates. Built to work with ChessBoardComputer
 * to utilize all these features to build the AI.
 * 
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;


public class ChessBoard extends JPanel {
    private GameWindow gameWindow;//sidewindow
    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private static final int TILE_SIZE = 80; 
    private static final int BOARD_SIZE = 8; 
    private String[][] pieceLocations = new String[BOARD_SIZE][BOARD_SIZE]; 
    private Map<String, Image> pieceImages = new HashMap<>();//store piece images
    private List<Point> validMoves = new ArrayList<>();
    private Point selectedPiece = null;//keeps track of piece
    private boolean isWhiteTurn = true;//keeps track of turns

    //castling
    private boolean hasWhiteKingMoved = false;
    private boolean hasBlackKingMoved = false;
    private boolean hasWhiteKingRookMoved = false;
    private boolean hasWhiteQueenRookMoved = false;
    private boolean hasBlackKingRookMoved = false;
    private boolean hasBlackQueenRookMoved = false;

    //en pasant
    private Point lastPawnDoubleMove = null;
    private int lastMoveNumber = 0; 
    private int currentMoveNumber = 0;

    //for the side window
    public void setGameWindow(GameWindow gameWindow) {
        this.gameWindow = gameWindow;
    }

    public ChessBoard() {
        loadPieceImages();
        drawPieces("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");//default position
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e.getPoint());
            }
        });
    }

    //found on stackoverflow while bugfixing
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    void handleMouseClick(Point click) {
        int col = click.x / TILE_SIZE;
        int row = click.y / TILE_SIZE;
    
        if (selectedPiece != null && validMoves.contains(new Point(col, row))) {
            String piece = pieceLocations[selectedPiece.y][selectedPiece.x];

            //convert into normal move 
            String moveNotation = String.format("%c%d to %c%d", 
                (char)('a' + selectedPiece.x), 8 - selectedPiece.y,
                (char)('a' + col), 8 - row);
            pcs.firePropertyChange("moveMade", null, moveNotation);
            
            //check for pawn promorion
            if (piece.replace("1", "").equals("pawn")) {
                if ((piece.equals("pawn") && row == 0) || (piece.equals("pawn1") && row == 7)) {
                    String promotedPiece = handlePawnPromotion(row, col, !piece.endsWith("1"));
                    if (promotedPiece != null) {
                        //update to new piece
                        piece = promotedPiece;
                    }
                }
            }
            
            //track if king has moved
            if (piece.replace("1", "").equals("king")) {
                if (isWhiteTurn) {
                    hasWhiteKingMoved = true;
                } else {
                    hasBlackKingMoved = true;
                }
                
                //castling moves
                if (Math.abs(col - selectedPiece.x) == 2) {
                    //kingside castling
                    if (col > selectedPiece.x) {
                        pieceLocations[row][col-1] = pieceLocations[row][7];
                        pieceLocations[row][7] = null;
                    }
                    //queenside castling
                    else {
                        pieceLocations[row][col+1] = pieceLocations[row][0];
                        pieceLocations[row][0] = null;
                    }
                }
            }
            
            //track if rook has moved
            if (piece.replace("1", "").equals("rook")) {
                if (selectedPiece.y == 0 || selectedPiece.y == 7) {
                    if (selectedPiece.x == 0) { //queen's rook
                        if (selectedPiece.y == 0) hasBlackQueenRookMoved = true;
                        else hasWhiteQueenRookMoved = true;
                    }
                    if (selectedPiece.x == 7) { //king's rook
                        if (selectedPiece.y == 0) hasBlackKingRookMoved = true;
                        else hasWhiteKingRookMoved = true;
                    }
                }
            }
    
            //en pasant capture
            if (piece.replace("1", "").equals("pawn") && 
                lastPawnDoubleMove != null && 
                col == lastPawnDoubleMove.x && 
                Math.abs(selectedPiece.x - col) == 1) {
                //remove captured piece
                pieceLocations[lastPawnDoubleMove.y][lastPawnDoubleMove.x] = null;
            }
    
            //track double move for en pasant
            if (piece.replace("1", "").equals("pawn") && Math.abs(row - selectedPiece.y) == 2) {
                lastPawnDoubleMove = new Point(col, row);
                lastMoveNumber = currentMoveNumber;
            } else {
                lastPawnDoubleMove = null;
            }
            currentMoveNumber++;
    
            //move piece to new location
            pieceLocations[row][col] = piece;
            pieceLocations[selectedPiece.y][selectedPiece.x] = null;
            selectedPiece = null;
            validMoves.clear();
            
            //switch turn
            isWhiteTurn = !isWhiteTurn;
        //generate moves for selected piece
        } else if (pieceLocations[row][col] != null) {
            boolean isWhitePiece = !pieceLocations[row][col].endsWith("1");
            if ((isWhiteTurn && isWhitePiece) || (!isWhiteTurn && !isWhitePiece)) {
                selectedPiece = new Point(col, row);
                validMoves = generateValidMoves(row, col);
            }
        } 
        //clear moves when piece deselected
        else {
            selectedPiece = null;
            validMoves.clear();
        }
        repaint();
        //if checkmate, show win message then back to intro screen using method from gamewindow
        if (checkmate(isWhiteTurn())) {
            JOptionPane.showMessageDialog(this, 
                isWhiteTurn() ? "Checkmate! Black wins!" : "Checkmate! White wins!");
            if (gameWindow != null) {
                gameWindow.handleGameEnd();
            }
        }
    }

    //load all black and white piece images
    public void loadPieceImages() {
        String[] pieceNames = {"rook", "knight", "bishop", "queen", "king", "pawn"};
        for (String piece : pieceNames) {
            pieceImages.put(piece, new ImageIcon(piece + ".png").getImage());
            pieceImages.put(piece + "1", new ImageIcon(piece + "1.png").getImage());
        }
        pieceImages.put("move", new ImageIcon("move.png").getImage());
    }


    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        //draw board with light and dark brown squares
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                boolean isLight = (row + col) % 2 == 0;
                g.setColor(isLight ? new Color(245, 222, 179) : new Color(139, 69, 19));
                g.fillRect(col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                String piece = pieceLocations[row][col];
                if (piece != null) {
                    Image pieceImage = pieceImages.get(piece);
                    if (pieceImage != null) {
                        g.drawImage(pieceImage, col * TILE_SIZE + 10, row * TILE_SIZE + 10, 60, 60, this);
                    }
                }
            }
        }

        //draw the markers for valid moves like where they can move
        for (Point move : validMoves) {
            g.drawImage(pieceImages.get("move"), move.x * TILE_SIZE + 20, move.y * TILE_SIZE + 20, 40, 40, this);
        }
    }

    //reset all flags and trackers as well as board and moves
    public void resetGame() {
        String[][] pieces = getPieceLocations();
        for (int i = 0; i < getBoardSize(); i++) {
            for (int j = 0; j < getBoardSize(); j++) {
                pieces[i][j] = null;
            }
        }

        //reset castling flags
        setHasWhiteKingMoved(false);
        setHasBlackKingMoved(false);
        setHasWhiteKingRookMoved(false);
        setHasWhiteQueenRookMoved(false);
        setHasBlackKingRookMoved(false);
        setHasBlackQueenRookMoved(false);

        drawPieces("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");
        setWhiteTurn(true);
        setSelectedPiece(null);
        getValidMoves().clear();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(BOARD_SIZE * TILE_SIZE, BOARD_SIZE * TILE_SIZE);
    }

    //draw the pieces on the board using fen string
    public void drawPieces(String fen) {
        String[] rows = fen.split("/");
        for (int row = 0; row < rows.length; row++) {
            int col = 0;
            for (char c : rows[row].toCharArray()) {
                if (Character.isDigit(c)) {
                    col += Character.getNumericValue(c);
                } else {
                    pieceLocations[row][col] = getPieceName(c);
                    col++;
                }
            }
        }
        repaint();
    }
    //check and handle pawn promotion, called when pawns reach last rank
    public String handlePawnPromotion(int row, int col, boolean isWhite) {
        //don't show if not at promotion rank
        if ((isWhite && row != 0) || (!isWhite && row != 7)) {
            return null;
        }
        
        //computer promotes to quen
        if (GraphicsEnvironment.isHeadless()) {
            return isWhite ? "queen" : "queen1";
        }
    
        //use promotion dialog window to select piece to promote to
        JFrame frame = (JFrame) SwingUtilities.getWindowAncestor(this);
        PromotionDialog dialog = new PromotionDialog(frame, isWhite, pieceImages);
        dialog.setVisible(true);
        
        return dialog.getSelectedPiece();
    }

    //get and return the name of the pieve
    public String getPieceName(char c) {
        boolean isWhite = Character.isUpperCase(c);
        char pieceChar = Character.toLowerCase(c);
        String pieceName;
        switch (pieceChar) {
            case 'r': pieceName = "rook"; break;
            case 'n': pieceName = "knight"; break;
            case 'b': pieceName = "bishop"; break;
            case 'q': pieceName = "queen"; break;
            case 'k': pieceName = "king"; break;
            case 'p': pieceName = "pawn"; break;
            default: throw new IllegalArgumentException("Invalid FEN character: " + c);
        }
        return isWhite ? pieceName : pieceName + "1";
    }

    //check for checkmades
    public boolean checkmate(boolean isWhiteTurn) {
        List<Point> allMoves = new ArrayList<>();
        //generates all moves
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                String piece = pieceLocations[row][col];
                if (piece == null) continue;
                boolean isWhitePiece = !piece.endsWith("1");
                if ((isWhiteTurn && isWhitePiece) || (!isWhiteTurn && !isWhitePiece)) {
                    allMoves.addAll(generateValidMoves(row, col));
                }
            }
        }

        // System.out.println(allMoves);
        //if no moves then checks for chec
        if (allMoves.isEmpty()) {

            if(!isKingInCheck(isWhiteTurn)){
                // System.out.println("stalemate");
            }
            return isKingInCheck(isWhiteTurn);
        }
        return false;
    }

    public List<Point> generateValidMoves(int row, int col) {
        List<Point> moves = new ArrayList<>();
        String piece = pieceLocations[row][col];
        
        if (piece == null) return moves;
        
        boolean isWhitePiece = !piece.endsWith("1");
        String pieceType = piece.replace("1", "");
        
        //generate basic moves
        if (pieceType.equals("king")) {
            //generate normal king moves
            moves.addAll(generateRawMoves(row, col, false));
            
            //add castling moves if conditions are met
            if (!isKingInCheck(isWhitePiece)) {
                //check kingside castling
                if (!isKingInCheck(isWhitePiece) && canCastleKingside(isWhitePiece)) {
                    moves.add(new Point(col + 2, row));
                }
                //check queenside castling
                if (!isKingInCheck(isWhitePiece) && canCastleQueenside(isWhitePiece)) {
                    moves.add(new Point(col - 2, row));
                }
            }
        } else {
            moves.addAll(generateRawMoves(row, col, true));
        }
        
        //filter moves that would leave king in check
        return filterMovesToResolveCheck(moves, row, col);
    }

    //generates all moves including ones that would leave king in check
    public List<Point> generateRawMoves(int row, int col, boolean checkCastling) {
        List<Point> moves = new ArrayList<>();
        String piece = pieceLocations[row][col];
        if (piece == null) return moves;
        
        int direction = piece.endsWith("1") ? 1 : -1;//sees if going up or down
        
        //generates moves for all cases 
        switch (piece.replace("1", "")) {
            //generates all pawn moves including captures
            case "pawn":
                if (isValidTile(row + direction, col) && pieceLocations[row + direction][col] == null) {
                    moves.add(new Point(col, row + direction));
                }
                if (direction == -1 && row == 6 && isValidTile(row - 2, col) && 
                    pieceLocations[row - 1][col] == null && pieceLocations[row - 2][col] == null) {
                    moves.add(new Point(col, row - 2));
                }
                if (direction == 1 && row == 1 && isValidTile(row + 2, col) && 
                    pieceLocations[row + 1][col] == null && pieceLocations[row + 2][col] == null) {
                    moves.add(new Point(col, row + 2));
                }
                if (isValidTile(row + direction, col + 1) && canCapture(row + direction, col + 1, !piece.endsWith("1"))) {
                    moves.add(new Point(col + 1, row + direction));
                }
                if (isValidTile(row + direction, col - 1) && canCapture(row + direction, col - 1, !piece.endsWith("1"))) {
                    moves.add(new Point(col - 1, row + direction));
                }
                if (lastPawnDoubleMove != null && lastMoveNumber == currentMoveNumber - 1) {
                    //for white pawns
                    if (!piece.endsWith("1") && row == 3) {
                        if ((col + 1 == lastPawnDoubleMove.x || col - 1 == lastPawnDoubleMove.x) && 
                            lastPawnDoubleMove.y == 3) {
                            moves.add(new Point(lastPawnDoubleMove.x, 2));
                        }
                    }
                    //for black pawns
                    else if (piece.endsWith("1") && row == 4) {
                        if ((col + 1 == lastPawnDoubleMove.x || col - 1 == lastPawnDoubleMove.x) && 
                            lastPawnDoubleMove.y == 4) {
                            moves.add(new Point(lastPawnDoubleMove.x, 5));
                        }
                    }
                }

                break;

            //generate rook moves for all directions
            case "rook":
                addLinearMoves(moves, row, col, 0, 1, true);
                addLinearMoves(moves, row, col, 0, -1, true);
                addLinearMoves(moves, row, col, 1, 0, true);
                addLinearMoves(moves, row, col, -1, 0, true);
                break;

            //generates bishop moves for all directions
            case "bishop":
                addDiagonalMoves(moves, row, col, 1, 1, true);
                addDiagonalMoves(moves, row, col, 1, -1, true);
                addDiagonalMoves(moves, row, col, -1, 1, true);
                addDiagonalMoves(moves, row, col, -1, -1, true);
                break;
            //generates queen moves using linear and diagonal move generation
            case "queen":
                addLinearMoves(moves, row, col, 0, 1, true);
                addLinearMoves(moves, row, col, 0, -1, true);
                addLinearMoves(moves, row, col, 1, 0, true);
                addLinearMoves(moves, row, col, -1, 0, true);
                addDiagonalMoves(moves, row, col, 1, 1, true);
                addDiagonalMoves(moves, row, col, 1, -1, true);
                addDiagonalMoves(moves, row, col, -1, 1, true);
                addDiagonalMoves(moves, row, col, -1, -1, true);
                break;

            //generates knight moves using offsets
            case "knight":
                int[][] knightMoves = {
                    {2, 1}, {2, -1}, {-2, 1}, {-2, -1},
                    {1, 2}, {1, -2}, {-1, 2}, {-1, -2}
                };
                for (int[] move : knightMoves) {
                    int newRow = row + move[0];
                    int newCol = col + move[1];
                    if (isValidTile(newRow, newCol)) {
                        String targetPiece = pieceLocations[newRow][newCol];
                        if (targetPiece == null || canCapture(newRow, newCol, !piece.endsWith("1"))) {
                            moves.add(new Point(newCol, newRow));
                        }
                    }
                }
                break;

            //generates king moves for all directions
            case "king":
                int[][] kingOffsets = {
                    {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                    {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
                };
                
                for (int[] offset : kingOffsets) {
                    int newRow = row + offset[0];
                    int newCol = col + offset[1];
                    if (isValidTile(newRow, newCol)) {
                        String targetPiece = pieceLocations[newRow][newCol];
                        if (targetPiece == null || canCapture(newRow, newCol, !piece.endsWith("1"))) {
                            moves.add(new Point(newCol, newRow));
                        }
                    }
                }
                break;
        }
        return moves;
    }

    //checks for kingside castling rights
    public boolean canCastleKingside(boolean isWhite) {
        int row = isWhite ? 7 : 0;
        
        if (isWhite && (hasWhiteKingMoved || hasWhiteKingRookMoved)) return false;
        if (!isWhite && (hasBlackKingMoved || hasBlackKingRookMoved)) return false;

        if (pieceLocations[row][5] != null || pieceLocations[row][6] != null) return false;

        if (isSquareUnderAttack(row, 4, isWhite) || 
            isSquareUnderAttack(row, 5, isWhite) || 
            isSquareUnderAttack(row, 6, isWhite)) return false;

        return true;
    }

    //checks for queenside castling rights
    public boolean canCastleQueenside(boolean isWhite) {
        int row = isWhite ? 7 : 0;
        
        if (isWhite && (hasWhiteKingMoved || hasWhiteQueenRookMoved)) return false;
        if (!isWhite && (hasBlackKingMoved || hasBlackQueenRookMoved)) return false;

        if (pieceLocations[row][1] != null || 
            pieceLocations[row][2] != null || 
            pieceLocations[row][3] != null) return false;

        if (isSquareUnderAttack(row, 4, isWhite) || 
            isSquareUnderAttack(row, 3, isWhite) || 
            isSquareUnderAttack(row, 2, isWhite)) return false;

        return true;
    }

    //checks if the square is attacked
    public boolean isSquareUnderAttack(int row, int col, boolean isWhite) {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                String piece = pieceLocations[r][c];
                if (piece != null && (piece.endsWith("1") == isWhite)) {
                    List<Point> moves = generateRawMoves(r, c, false);
                    if (moves.contains(new Point(col, row))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //checks if you can capture this piece
    public boolean canCapture(int row, int col, boolean isWhitePawn) {
        String targetPiece = pieceLocations[row][col];
        if (targetPiece == null) return false;
        boolean isWhiteTarget = !targetPiece.endsWith("1");
        return isWhitePawn != isWhiteTarget;
    }

    //finds straight linear moves for rook and queen move generation
    public void addLinearMoves(List<Point> moves, int row, int col, int rowStep, int colStep, boolean stopAtBlock) {
        int currentRow = row + rowStep;
        int currentCol = col + colStep;
    
        String selectedPiece = pieceLocations[row][col];
        boolean isWhiteSelected = !selectedPiece.endsWith("1");
    
        //adds moves until there is something in the way 
        while (isValidTile(currentRow, currentCol)) {
            String targetPiece = pieceLocations[currentRow][currentCol];
            if (targetPiece != null) {
                boolean isWhiteTarget = !targetPiece.endsWith("1");
                if (isWhiteSelected == isWhiteTarget) {
                    break;
                }
                if (stopAtBlock) {
                    moves.add(new Point(currentCol, currentRow));
                }
                break;
            }
            moves.add(new Point(currentCol, currentRow));
            currentRow += rowStep;
            currentCol += colStep;
        }
    }

    //finds diagonal moves to be used for move generation for queen and bishop
    public void addDiagonalMoves(List<Point> moves, int row, int col, int rowStep, int colStep, boolean stopAtBlock) {
        int currentRow = row + rowStep;
        int currentCol = col + colStep;
    
        String selectedPiece = pieceLocations[row][col];
        boolean isWhiteSelected = !selectedPiece.endsWith("1");
    
        //adds moves until there is something in the way 
        while (isValidTile(currentRow, currentCol)) {
            String targetPiece = pieceLocations[currentRow][currentCol];
            if (targetPiece != null) {
                boolean isWhiteTarget = !targetPiece.endsWith("1");
                if (isWhiteSelected == isWhiteTarget) {
                    break;
                }
                if (stopAtBlock) {
                    moves.add(new Point(currentCol, currentRow));
                }
                break;
            }
            moves.add(new Point(currentCol, currentRow));
            currentRow += rowStep;
            currentCol += colStep;
        }
    }

    //checks if tile is on the board
    public boolean isValidTile(int row, int col) {
        return row >= 0 && row < BOARD_SIZE && col >= 0 && col < BOARD_SIZE;
    }

    //check if king is in check
    public boolean isKingInCheck(boolean isWhiteKing) {
        Point kingPosition = findKing(isWhiteKing);
        if (kingPosition == null) {
            throw new IllegalStateException("King not found");
        }
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                String piece = pieceLocations[row][col];
                if (piece != null && piece.endsWith(isWhiteKing ? "1" : "")) {
                    List<Point> opponentMoves = generateRawMoves(row, col, false);
                    if (opponentMoves.contains(kingPosition)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    //return position of king on the board
    public Point findKing(boolean isWhiteKing) {
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                String piece = pieceLocations[row][col];
                if (piece != null && piece.equals(isWhiteKing ? "king" : "king1")) {
                    return new Point(col, row);
                }
            }
        }
        return null;
    }

    //checks what moves can be made for the check to be solved
    public List<Point> filterMovesToResolveCheck(List<Point> moves, int row, int col) {
        List<Point> filteredMoves = new ArrayList<>();
        String originalPiece = pieceLocations[row][col];
        boolean isWhitePiece = !originalPiece.endsWith("1");
        
        for (Point move : moves) {
            //safety inbound check
            if (move.y < 0 || move.y >= BOARD_SIZE || move.x < 0 || move.x >= BOARD_SIZE) {
                continue;  //skip invalid moves
            }
            
            int targetRow = move.y;
            int targetCol = move.x;
            
            //store original state
            String capturedPiece = pieceLocations[targetRow][targetCol];
            
            //simulate move
            pieceLocations[targetRow][targetCol] = originalPiece;
            pieceLocations[row][col] = null;
            
            //special check for castling
            boolean isCastlingMove = originalPiece.replace("1", "").equals("king") && 
                                    Math.abs(targetCol - col) == 2;
            
            if (isCastlingMove) {
                int rookRow = isWhitePiece ? 7 : 0;
                if (targetCol > col) { //kingside castling
                    if (targetCol < BOARD_SIZE - 1) {
                        pieceLocations[rookRow][targetCol - 1] = pieceLocations[rookRow][7];
                        pieceLocations[rookRow][7] = null;
                    }
                } else { //queenside castling
                    if (targetCol > 0) {
                        pieceLocations[rookRow][targetCol + 1] = pieceLocations[rookRow][0];
                        pieceLocations[rookRow][0] = null;
                    }
                }
            }
            
            //check if the move is legal and doesn't leave king in check
            if (!isKingInCheck(isWhitePiece)) {
                filteredMoves.add(move);
            }
            
            //restore original position
            pieceLocations[row][col] = originalPiece;
            pieceLocations[targetRow][targetCol] = capturedPiece;
            
            //restore rook position if it was a castling move
            if (isCastlingMove) {
                int rookRow = isWhitePiece ? 7 : 0;
                if (targetCol > col) { //kingside castling
                    pieceLocations[rookRow][7] = isWhitePiece ? "rook" : "rook1";
                    pieceLocations[rookRow][targetCol - 1] = null;
                } else { //queenside castling
                    pieceLocations[rookRow][0] = isWhitePiece ? "rook" : "rook1";
                    pieceLocations[rookRow][targetCol + 1] = null;
                }
            }
        }
        
        return filteredMoves;
    }

    //getter and setter methods
    public String[][] getPieceLocations() { return pieceLocations; }
    public int getTileSize() { return TILE_SIZE; }
    public int getBoardSize() { return BOARD_SIZE; }
    public void setPieceLocations(String[][] pieceLocations) { this.pieceLocations = pieceLocations; }
    public Map<String, Image> getPieceImages() { return pieceImages; }
    public void setPieceImages(Map<String, Image> pieceImages) { this.pieceImages = pieceImages; }
    public List<Point> getValidMoves() { return validMoves; }
    public void setValidMoves(List<Point> validMoves) { this.validMoves = validMoves; }
    public Point getSelectedPiece() { return selectedPiece; }
    public void setSelectedPiece(Point selectedPiece) { this.selectedPiece = selectedPiece; }
    public boolean isWhiteTurn() { return isWhiteTurn; }
    public void setWhiteTurn(boolean whiteTurn) { isWhiteTurn = whiteTurn; }
    public void setHasWhiteKingMoved(boolean hasWhiteKingMoved) {this.hasWhiteKingMoved = hasWhiteKingMoved;}
    public void setHasBlackKingMoved(boolean hasBlackKingMoved) {this.hasBlackKingMoved = hasBlackKingMoved;}
    public void setHasWhiteKingRookMoved(boolean hasWhiteKingRookMoved) {this.hasWhiteKingRookMoved = hasWhiteKingRookMoved;}
    public void setHasWhiteQueenRookMoved(boolean hasWhiteQueenRookMoved) {this.hasWhiteQueenRookMoved = hasWhiteQueenRookMoved;}
    public void setHasBlackKingRookMoved(boolean hasBlackKingRookMoved) {this.hasBlackKingRookMoved = hasBlackKingRookMoved;}
    public void setHasBlackQueenRookMoved(boolean hasBlackQueenRookMoved) {this.hasBlackQueenRookMoved = hasBlackQueenRookMoved;}


    public long perft(int depth) {
        if (depth == 0) {
            return 1;
        }
    
        long nodes = 0;
        List<Move> moves = generateAllMoves();
    
        for (Move move : moves) {
            //make move
            String capturedPiece = makeMove(move);
            
            //get nodes from position
            nodes += perft(depth - 1);
            
            //unmake move
            unmakeMove(move, capturedPiece);
        }
    
        return nodes;
    }
    
    //helper class to store moves
    private class Move {
        int fromRow, fromCol, toRow, toCol;
        String piece;
        
        public Move(int fromRow, int fromCol, int toRow, int toCol, String piece) {
            this.fromRow = fromRow;
            this.fromCol = fromCol;
            this.toRow = toRow;
            this.toCol = toCol;
            this.piece = piece;
        }
    }
    
    //generate all possible moves for a current position
    private List<Move> generateAllMoves() {
        List<Move> allMoves = new ArrayList<>();
        boolean currentTurn = isWhiteTurn;
        //loop through to get all pieces to generate moves 
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                String piece = pieceLocations[row][col];
                if (piece != null) {
                    boolean isWhitePiece = !piece.endsWith("1");
                    if (currentTurn == isWhitePiece) {
                        List<Point> moves = generateValidMoves(row, col);
                        for (Point move : moves) {
                            allMoves.add(new Move(row, col, move.y, move.x, piece));
                        }
                    }
                }
            }
        }
        return allMoves;
    }
    
    //make move and return the captured piece
    private String makeMove(Move move) {
        String capturedPiece = pieceLocations[move.toRow][move.toCol];
        pieceLocations[move.toRow][move.toCol] = move.piece;
        pieceLocations[move.fromRow][move.fromCol] = null;
        isWhiteTurn = !isWhiteTurn;
        return capturedPiece;
    }
    
    //undo move
    private void unmakeMove(Move move, String capturedPiece) {
        pieceLocations[move.fromRow][move.fromCol] = move.piece;
        pieceLocations[move.toRow][move.toCol] = capturedPiece;
        isWhiteTurn = !isWhiteTurn;
    }
    
    //performance test to see if generates correct num of moves
    public void runPerft() {
        System.out.println("Starting Perft test...");
        for (int depth = 1; depth <= 4; depth++) {
            long startTime = System.currentTimeMillis();
            long nodes = perft(depth);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            System.out.printf("Depth %d: %d moves (%d ms)%n", depth, nodes, duration);
        }
    }

    //just for testing, should be ran through introscreen
    public static void main(String[] args) {
        JFrame frame = new JFrame("Chess Board");
        ChessBoard board = new ChessBoard();
        // board.runPerft();
        board.drawPieces("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR");
        frame.add(board);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}


//window to come up when pawn promoted to select what to promote to
class PromotionDialog extends JDialog {
    private String selectedPiece = null;
    private final boolean isWhite;
    private final Map<String, Image> pieceImages;

    public PromotionDialog(JFrame parent, boolean isWhite, Map<String, Image> pieceImages) {
        super(parent, "Promote Pawn", true);
        this.isWhite = isWhite;
        this.pieceImages = pieceImages;
        setupDialog();
    }

    private void setupDialog() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        String[] pieces = {"queen", "rook", "bishop", "knight"};
        
        for (String piece : pieces) {
            JButton button = createPieceButton(piece);
            panel.add(button);
        }

        setContentPane(panel);
        pack();
        setLocationRelativeTo(getParent());
    }

    private JButton createPieceButton(String piece) {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                String imagePiece = isWhite ? piece : piece + "1";
                Image img = pieceImages.get(imagePiece);
                if (img != null) {
                    g.drawImage(img, 10, 10, 60, 60, this);
                }
            }
        };
        
        button.setPreferredSize(new Dimension(80, 80));
        button.addActionListener(e -> {
            selectedPiece = isWhite ? piece : piece + "1";
            dispose();
        });
        
        return button;
    }

    //getter
    public String getSelectedPiece() {
        return selectedPiece;
    }
}