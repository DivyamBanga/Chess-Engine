/**
 * IntroScreen.java
 * 
 * Divyam Banga
 * ICS4U, Mr. Mckenzie
 * 
 * Introduction screen/main menu with options for player vs player and player vs computer. Contains a time bar to select the time of the game
 * which works perfectly in player vs player with some slight bugs in player vs computer.
 * 
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class IntroScreen extends JFrame {
    private JSlider timeSlider;
    private JLabel timeLabel;
    private int timeLimit = 300; //default 5 mins
    private BufferedImage backgroundImage;

    //background
    class BackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (backgroundImage != null) {
                //draw
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        }
    }

    public IntroScreen() {
        setTitle("Chess Intro Screen");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        //creaye panel
        BackgroundPanel mainPanel = new BackgroundPanel();
        mainPanel.setLayout(new GridBagLayout());
        setContentPane(mainPanel);

        GridBagConstraints gbc = new GridBagConstraints();

        //load background image
        try {
            backgroundImage = ImageIO.read(new File("back.png"));
        } catch (IOException e) {
            System.err.println("Error loading background image: " + e.getMessage());
        }

        //time settings panel
        JPanel timePanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(255, 255, 255, 200)); 
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        //formatting time bar
        timePanel.setOpaque(false);
        timeSlider = new JSlider(JSlider.HORIZONTAL, 0, 600, 300);
        timeSlider.setMajorTickSpacing(120);
        timeSlider.setMinorTickSpacing(60);
        timeSlider.setPaintTicks(true);
        timeSlider.setPaintLabels(true);

        timeLabel = new JLabel("Time: 5:00 minutes");
        timeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        //change display
        timeSlider.addChangeListener(e -> {
            timeLimit = timeSlider.getValue();
            int minutes = timeLimit / 60;
            int seconds = timeLimit % 60;
            timeLabel.setText(String.format("Time: %d:%02d minutes", minutes, seconds));
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);//border
        timePanel.add(timeLabel, gbc);

        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        timePanel.add(timeSlider, gbc);

        //main panel
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(new Color(255, 255, 255, 200));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        buttonPanel.setOpaque(false);

        //buttons
        JButton playerVsPlayerButton = createStyledButton("Player vs Player");
        JButton playerVsComputerButton = createStyledButton("Player vs Computer");

        playerVsPlayerButton.addActionListener(e -> {
            ChessBoard chessBoard = new ChessBoard();
            GameWindow gameWindow = new GameWindow("Player vs Player", chessBoard, timeLimit, this);
            chessBoard.setGameWindow(gameWindow);
            gameWindow.setVisible(true);
            this.setVisible(false);
        });
    
        playerVsComputerButton.addActionListener(e -> {
            ChessBoardComputer chessBoardComputer = new ChessBoardComputer(true);
            GameWindow gameWindow = new GameWindow("Player vs Computer", chessBoardComputer, timeLimit, this);
            chessBoardComputer.setGameWindow(gameWindow);
            gameWindow.setVisible(true);
            this.setVisible(false);
        });

        buttonPanel.add(playerVsPlayerButton);
        buttonPanel.add(playerVsComputerButton);

        //add panels ro frame
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(20, 20, 10, 20);
        mainPanel.add(timePanel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(10, 20, 20, 20);
        mainPanel.add(buttonPanel, gbc);

        setLocationRelativeTo(null);
    }

    //button with hover effect
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setForeground(Color.WHITE);
        button.setBackground(new Color(70, 130, 180));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        //hover
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(new Color(100, 149, 237));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(new Color(70, 130, 180));
            }
        });
        
        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new IntroScreen().setVisible(true));
    }
}