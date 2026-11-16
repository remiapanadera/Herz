
import gui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {

        // Start the application on Swing's Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {

            try {
                // Set a consistent cross-platform Swing appearance
                UIManager.setLookAndFeel(
                        UIManager.getCrossPlatformLookAndFeelClassName()
                );

            } catch (Exception e) {
                System.err.println(
                        "Unable to set application appearance: "
                                + e.getMessage()
                );
            }

            // Open the Herz Hotel login screen
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);

        });
    }
}
