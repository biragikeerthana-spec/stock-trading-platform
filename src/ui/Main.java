package ui;

import util.DataManager;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Application entry point. Loads saved state, configures the look-and-feel,
 * and launches the Dashboard on the EDT.
 */
public class Main {

    public static void main(String[] args) {
        // Use the system look-and-feel for a native, modern appearance.
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fall back to the default Metal L&F.
            System.err.println("Could not set system look-and-feel: " + e.getMessage());
        }

        SwingUtilities.invokeLater(() -> {
            var service = DataManager.loadInitialState();
            Dashboard dashboard = new Dashboard(service);
            dashboard.setVisible(true);
        });
    }
}
