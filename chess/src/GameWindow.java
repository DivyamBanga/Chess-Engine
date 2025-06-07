/**
 * GameWindow.java
 * 
 * Divyam Banga
 * ICS4U, Mr. Mckenzie
 * 
 * Sidewindow to show the depth and computer moves in player vs computer as well as both user moves in player vs player.
 * 
 */

import javax.swing.*;
import java.awt.*;


public class GameWindow extends JFrame {
    private JLabel TimeLabel;
    private JTextArea gameLog;
    private Timer whiteTimer;
    private Timer blackTimer;
    private int whiteTimeLeft;
    private int blackTimeLeft;
    private ChessBoard chessBoard;
    private boolean isComputerThinking = false;
    private IntroScreen introScreen;

    public GameWindow(String title, ChessBoard board, int timeLimit, IntroScreen introScreen) {

        super(title);
        this.introScreen = introScreen;
        this.chessBoard = board;
        this.whiteTimeLeft = timeLimit;
        this.blackTimeLeft = timeLimit;
        

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        //main game panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(chessBoard, BorderLayout.CENTER);

        //side panel
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setPreferredSize(new Dimension(200, 0));
        
        //time labels
        JPanel timePanel = new JPanel(new GridLayout(2, 1));
        TimeLabel = new JLabel("White: " + formatTime(whiteTimeLeft) + "        Black: " + formatTime(blackTimeLeft));
        timePanel.add(TimeLabel);
        statusPanel.add(timePanel, BorderLayout.NORTH);

        //gaem log
        gameLog = new JTextArea();
        gameLog.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(gameLog);
        scrollPane.setPreferredSize(new Dimension(200, 400));
        statusPanel.add(scrollPane, BorderLayout.CENTER);

        //add panels to frame
        add(mainPanel, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.EAST);

        setupTimers();

        //start whites timer
        whiteTimer.start();

        pack();
        setLocationRelativeTo(null);

        //move logging
        board.addPropertyChangeListener("moveMade", evt -> {
            String move = (String) evt.getNewValue();
            if (board instanceof ChessBoardComputer) {
                ChessBoardComputer computerBoard = (ChessBoardComputer) board;
                System.out.println(computerBoard.count);
                int depth = computerBoard.getLastSearchDepth();
                logMove(move + " (Depth: " + depth + ")");
                
                //computer thinking
                if (isComputerThinking) {
                    isComputerThinking = false;
                }
            } else {
                logMove(move);
            }
            switchTimers();
        });

        //checks if its aa computer game and converts to computer board
        if (board instanceof ChessBoardComputer) {
            ChessBoardComputer computerBoard = (ChessBoardComputer) board;
            computerBoard.addPropertyChangeListener("computerThinking", evt -> {
                isComputerThinking = (Boolean) evt.getNewValue();
            });
        }
    }

    //sets up, manages, and checks timer conditions
    private void setupTimers() {
        whiteTimer = new Timer(1000, e -> {
            if (whiteTimeLeft > 0) {
                whiteTimeLeft--;
                updateTimeLabel();
            } else {
                ((Timer) e.getSource()).stop();
                JOptionPane.showMessageDialog(this, "Black wins on time!");
                handleGameEnd();
                resetTimers();
            }
        });
    
        blackTimer = new Timer(1000, e -> {
            if (blackTimeLeft > 0) {
                blackTimeLeft--;
                updateTimeLabel();
            } else {
                ((Timer) e.getSource()).stop();
                JOptionPane.showMessageDialog(this, "White wins on time!");
                handleGameEnd();
                resetTimers();
            }
        });
    }


    private void updateTimeLabel() {
        TimeLabel.setText("White: " + formatTime(whiteTimeLeft) + 
                         "        Black: " + formatTime(blackTimeLeft));
    }

    //close current window and go back to introscreen
    private void resetToIntroScreen() {
        whiteTimer.stop();
        blackTimer.stop();
        this.dispose();
        introScreen.setVisible(true);
    }

    //reset
    public void handleGameEnd() {
        resetToIntroScreen();
    }

    //used to switch turns and timers
    private void switchTimers() {
        if (whiteTimer.isRunning()) {
            whiteTimer.stop();
            blackTimer.start();
        } else {
            blackTimer.stop();
            whiteTimer.start();
        }
    }


    //stops and resets timer
    private void resetTimers() {
        whiteTimer.stop();
        blackTimer.stop();
        whiteTimeLeft = blackTimeLeft = 300; //default to 5 mins
        TimeLabel.setText("White: " + formatTime(whiteTimeLeft)+"       Black: " + formatTime(blackTimeLeft));
        whiteTimer.start();
    }

    //formats to mins:secs
    private String formatTime(int seconds) {
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    //logs move to window
    private void logMove(String move) {
        gameLog.append(move + "\n");
        gameLog.setCaretPosition(gameLog.getDocument().getLength());
    }
}