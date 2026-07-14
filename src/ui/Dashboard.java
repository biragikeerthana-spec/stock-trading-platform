package ui;

import model.Holding;
import model.Stock;
import model.Transaction;
import service.TradingService;
import util.DataManager;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.List;

/**
 * Main application window. Hosts the dashboard header, the tabbed content
 * area (Market / Portfolio / Transactions) and the action toolbar.
 *
 * Business logic lives entirely in TradingService; this class is responsible
 * only for presentation and user interaction.
 */
public class Dashboard extends JFrame {

    private static final int REFRESH_INTERVAL_MS = 3000;

    private final TradingService service;

    // Header labels
    private JLabel lblWelcome;
    private JLabel lblBalance;
    private JLabel lblPortfolioValue;
    private JLabel lblProfitLoss;

    // Market tab controls
    private JTextField txtSearch;
    private final DefaultTableModel marketModel = new DefaultTableModel(
            new Object[]{"Symbol", "Company", "Price (₹)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private JTable marketTable;

    // Portfolio tab
    private final DefaultTableModel portfolioModel = new DefaultTableModel(
            new Object[]{"Stock", "Qty", "Avg Buy (₹)", "Current (₹)", "Investment (₹)", "Value (₹)", "P/L (₹)"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private JTable portfolioTable;

    // Transactions tab
    private final DefaultTableModel txModel = new DefaultTableModel(
            new Object[]{"Type", "Symbol", "Qty", "Price (₹)", "Date & Time"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private JTable txTable;

    // Graph panel
    private GraphPanel graphPanel;

    // Timer for the market simulation
    private Timer marketTimer;

    // Theme state
    private boolean darkMode = false;

    // Colors for the light theme
    private static final Color LIGHT_BG = new Color(245, 247, 250);
    private static final Color LIGHT_PANEL = Color.WHITE;
    private static final Color LIGHT_TEXT = new Color(33, 37, 41);
    private static final Color LIGHT_ACCENT = new Color(13, 110, 253);

    // Colors for the dark theme
    private static final Color DARK_BG = new Color(24, 26, 31);
    private static final Color DARK_PANEL = new Color(34, 37, 43);
    private static final Color DARK_TEXT = new Color(220, 224, 230);
    private static final Color DARK_ACCENT = new Color(88, 166, 255);

    public Dashboard(TradingService service) {
        this.service = service;
        setTitle("Stock Trading Platform");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        buildLayout();
        applyTheme();
        refreshAll();

        startMarketSimulation();

        // Save state when the window is closed.
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                DataManager.saveAll(Dashboard.this.service);
            }
        });
    }

    // ------------------------------------------------------------------
    // Layout construction
    // ------------------------------------------------------------------

    private void buildLayout() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.setBackground(LIGHT_BG);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildTabs(), BorderLayout.CENTER);
        root.add(buildToolbar(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    /** Dashboard header: welcome + balance / portfolio value / P&L cards. */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        lblWelcome = new JLabel("Welcome, " + service.getUser().getName());
        lblWelcome.setFont(new Font("SansSerif", Font.BOLD, 20));

        JPanel cards = new JPanel(new GridLayout(1, 3, 10, 0));
        cards.setOpaque(false);
        lblBalance = makeCardLabel("Available Cash", "₹0.00");
        lblPortfolioValue = makeCardLabel("Portfolio Value", "₹0.00");
        lblProfitLoss = makeCardLabel("Profit / Loss", "₹0.00");
        cards.add(wrapCard(lblBalance));
        cards.add(wrapCard(lblPortfolioValue));
        cards.add(wrapCard(lblProfitLoss));

        header.add(lblWelcome, BorderLayout.NORTH);
        header.add(cards, BorderLayout.CENTER);
        return header;
    }

    private JLabel makeCardLabel(String title, String value) {
        JLabel label = new JLabel("<html><div style='text-align:center'>"
                + "<div style='font-size:11px'>" + title + "</div>"
                + "<div style='font-size:16px;font-weight:bold'>" + value + "</div></div></html>");
        label.setHorizontalAlignment(JLabel.CENTER);
        label.putClientProperty("title", title);
        return label;
    }

    private JPanel wrapCard(JLabel label) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220), 1));
        card.add(label, BorderLayout.CENTER);
        return card;
    }

    /** Tabbed pane with Market, Portfolio and Transactions tabs. */
    private JComponent buildTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Market", buildMarketTab());
        tabs.addTab("Portfolio", buildPortfolioTab());
        tabs.addTab("Transactions", buildTransactionsTab());
        return tabs;
    }

    // ---- Market tab ----------------------------------------------------

    private JPanel buildMarketTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        // Search + sort bar
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        txtSearch = new JTextField(12);
        txtSearch.setToolTipText("Search by symbol or company name");
        JButton btnSearch = new JButton("Search");
        JButton btnClear = new JButton("Clear");
        JButton btnSortPrice = new JButton("Sort by Price");
        JButton btnSortCompany = new JButton("Sort by Company");

        btnSearch.addActionListener(e -> refreshMarketTable(service.getMarket().search(txtSearch.getText())));
        btnClear.addActionListener(e -> { txtSearch.setText(""); refreshMarketTable(); });
        btnSortPrice.addActionListener(e -> refreshMarketTable(service.getMarket().sortByPrice(false)));
        btnSortCompany.addActionListener(e -> refreshMarketTable(service.getMarket().sortByCompany()));

        topBar.add(new JLabel("Search:"));
        topBar.add(txtSearch);
        topBar.add(btnSearch);
        topBar.add(btnClear);
        topBar.add(btnSortPrice);
        topBar.add(btnSortCompany);

        marketTable = new JTable(marketModel);
        marketTable.setRowHeight(24);
        marketTable.setAutoCreateRowSorter(true);
        styleTableHeader(marketTable.getTableHeader());

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(marketTable), BorderLayout.CENTER);

        // Double-click a row to buy quickly.
        marketTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = marketTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        String symbol = (String) marketModel.getValueAt(marketTable.convertRowIndexToModel(row), 0);
                        promptBuy(symbol);
                    }
                }
            }
        });

        return panel;
    }

    // ---- Portfolio tab ------------------------------------------------

    private JPanel buildPortfolioTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        portfolioTable = new JTable(portfolioModel);
        portfolioTable.setRowHeight(24);
        styleTableHeader(portfolioTable.getTableHeader());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSell = new JButton("Sell Selected");
        btnSell.addActionListener(e -> sellSelected());
        bottom.add(btnSell);

        graphPanel = new GraphPanel();
        graphPanel.setPreferredSize(new Dimension(0, 160));

        JPanel right = new JPanel(new BorderLayout());
        right.add(new JLabel("Profit / Loss Trend", JLabel.CENTER), BorderLayout.NORTH);
        right.add(graphPanel, BorderLayout.CENTER);

        JSplitPaneFix split = new JSplitPaneFix(
                new JScrollPane(portfolioTable), right);
        split.setResizeWeight(0.65);

        panel.add(split, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // ---- Transactions tab ---------------------------------------------

    private JPanel buildTransactionsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));

        txTable = new JTable(txModel);
        txTable.setRowHeight(24);
        styleTableHeader(txTable.getTableHeader());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnExport = new JButton("Export to CSV");
        btnExport.addActionListener(e -> exportCsv());
        bottom.add(btnExport);

        panel.add(new JScrollPane(txTable), BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    // ---- Bottom toolbar -----------------------------------------------

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton btnBuy = new JButton("Buy");
        JButton btnSell = new JButton("Sell");
        JButton btnRefresh = new JButton("Refresh");
        JButton btnSave = new JButton("Save");
        JButton btnLoad = new JButton("Load");
        JButton btnDark = new JButton("Dark Mode");

        btnBuy.addActionListener(e -> promptBuy(null));
        btnSell.addActionListener(e -> sellSelected());
        btnRefresh.addActionListener(e -> { service.getMarket().simulatePriceChanges(); refreshAll(); });
        btnSave.addActionListener(e -> {
            DataManager.saveAll(service);
            JOptionPane.showMessageDialog(this, "Data saved successfully.");
        });
        btnLoad.addActionListener(e -> {
            TradingService loaded = DataManager.loadInitialState();
            replaceService(loaded);
            JOptionPane.showMessageDialog(this, "Data loaded successfully.");
        });
        btnDark.addActionListener(e -> toggleDarkMode());

        bar.add(btnBuy);
        bar.add(btnSell);
        bar.add(btnRefresh);
        bar.add(btnSave);
        bar.add(btnLoad);
        bar.add(btnDark);
        return bar;
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    /** Opens a dialog to buy a stock. Pre-fills the symbol when provided. */
    private void promptBuy(String presetSymbol) {
        List<Stock> stocks = service.getMarket().getAllStocks();
        if (stocks.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No stocks available.");
            return;
        }

        JComboBox<String> combo = new JComboBox<>();
        for (Stock s : stocks) {
            combo.addItem(s.getSymbol() + " - " + s.getCompanyName() + " (₹" + format(s.getCurrentPrice()) + ")");
        }
        if (presetSymbol != null) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (combo.getItemAt(i).startsWith(presetSymbol + " -")) {
                    combo.setSelectedIndex(i);
                    break;
                }
            }
        }

        JTextField qtyField = new JTextField("1");

        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Stock:"));
        panel.add(combo);
        panel.add(new JLabel("Quantity:"));
        panel.add(qtyField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Buy Stock",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        int selected = combo.getSelectedIndex();
        String symbol = stocks.get(selected).getSymbol();
        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity must be a whole number.");
            return;
        }

        String error = service.buy(symbol, qty);
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Buy Failed", JOptionPane.WARNING_MESSAGE);
        } else {
            refreshAll();
        }
    }

    /** Sells the holding currently selected in the portfolio table. */
    private void sellSelected() {
        int row = portfolioTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a holding to sell.");
            return;
        }
        int modelRow = portfolioTable.convertRowIndexToModel(row);
        String symbol = (String) portfolioModel.getValueAt(modelRow, 0);
        int owned = (int) portfolioModel.getValueAt(modelRow, 1);

        JTextField qtyField = new JTextField(String.valueOf(owned));
        JPanel panel = new JPanel(new GridLayout(2, 2, 8, 8));
        panel.add(new JLabel("Stock:"));
        panel.add(new JLabel(symbol + " (owned: " + owned + ")"));
        panel.add(new JLabel("Quantity:"));
        panel.add(qtyField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Sell Stock",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity must be a whole number.");
            return;
        }

        String error = service.sell(symbol, qty);
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Sell Failed", JOptionPane.WARNING_MESSAGE);
        } else {
            refreshAll();
        }
    }

    private void exportCsv() {
        if (service.getTransactions().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No transactions to export.");
            return;
        }
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setSelectedFile(new File("transactions.csv"));
        if (chooser.showSaveDialog(this) == javax.swing.JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                int rows = DataManager.exportTransactionsCsv(service.getTransactions(), file.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Exported " + rows + " rows to\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage());
            }
        }
    }

    private void toggleDarkMode() {
        darkMode = !darkMode;
        applyTheme();
    }

    // ------------------------------------------------------------------
    // Market simulation
    // ------------------------------------------------------------------

    private void startMarketSimulation() {
        marketTimer = new Timer(REFRESH_INTERVAL_MS, e -> {
            service.getMarket().simulatePriceChanges();
            refreshAll();
        });
        marketTimer.start();
    }

    // ------------------------------------------------------------------
    // Refresh / data binding
    // ------------------------------------------------------------------

    /** Refreshes every table and header label from the service. */
    private void refreshAll() {
        refreshHeader();
        refreshMarketTable();
        refreshPortfolioTable();
        refreshTransactionTable();
        graphPanel.addPoint(service.profitLoss());
    }

    private void refreshHeader() {
        lblWelcome.setText("Welcome, " + service.getUser().getName());
        lblBalance.setText(cardText("Available Cash", "₹" + format(service.availableCash())));
        lblPortfolioValue.setText(cardText("Portfolio Value", "₹" + format(service.portfolioValue())));
        double pl = service.profitLoss();
        String plText = (pl >= 0 ? "+" : "") + "₹" + format(pl);
        lblProfitLoss.setText(cardText("Profit / Loss", plText));
        lblProfitLoss.setForeground(pl >= 0 ? new Color(21, 128, 61) : new Color(185, 28, 28));
    }

    private String cardText(String title, String value) {
        return "<html><div style='text-align:center'>"
                + "<div style='font-size:11px'>" + title + "</div>"
                + "<div style='font-size:16px;font-weight:bold'>" + value + "</div></div></html>";
    }

    private void refreshMarketTable() {
        refreshMarketTable(service.getMarket().getAllStocks());
    }

    private void refreshMarketTable(List<Stock> stocks) {
        marketModel.setRowCount(0);
        for (Stock s : stocks) {
            marketModel.addRow(new Object[]{s.getSymbol(), s.getCompanyName(), format(s.getCurrentPrice())});
        }
    }

    private void refreshPortfolioTable() {
        portfolioModel.setRowCount(0);
        for (Holding h : service.getPortfolio().getHoldings()) {
            Stock s = service.getMarket().getStock(h.getSymbol());
            double current = (s != null) ? s.getCurrentPrice() : h.getAverageBuyPrice();
            double investment = h.getQuantity() * h.getAverageBuyPrice();
            double value = h.getQuantity() * current;
            double pl = value - investment;
            portfolioModel.addRow(new Object[]{
                    h.getSymbol(),
                    h.getQuantity(),
                    format(h.getAverageBuyPrice()),
                    format(current),
                    format(investment),
                    format(value),
                    (pl >= 0 ? "+" : "") + format(pl)
            });
        }
    }

    private void refreshTransactionTable() {
        txModel.setRowCount(0);
        for (Transaction t : service.getTransactions()) {
            txModel.addRow(new Object[]{
                    t.getType(),
                    t.getSymbol(),
                    t.getQuantity(),
                    format(t.getPrice()),
                    t.getFormattedDateTime()
            });
        }
    }

    // ------------------------------------------------------------------
    // Theme
    // ------------------------------------------------------------------

    private void applyTheme() {
        Color bg = darkMode ? DARK_BG : LIGHT_BG;
        Color panel = darkMode ? DARK_PANEL : LIGHT_PANEL;
        Color text = darkMode ? DARK_TEXT : LIGHT_TEXT;
        Color accent = darkMode ? DARK_ACCENT : LIGHT_ACCENT;

        JPanel root = (JPanel) getContentPane();
        root.setBackground(bg);

        for (Component c : root.getComponents()) {
            applyThemeRecursive(c, bg, panel, text, accent);
        }
        SwingUtilities.updateComponentTreeUI(this);
    }

    @SuppressWarnings("unchecked")
    private void applyThemeRecursive(Component c, Color bg, Color panel, Color text, Color accent) {
        if (c instanceof JPanel p) {
            p.setBackground(panel);
            for (Component child : p.getComponents()) {
                applyThemeRecursive(child, bg, panel, text, accent);
            }
        } else if (c instanceof JLabel l) {
            l.setForeground(text);
        } else if (c instanceof JTable t) {
            t.setBackground(panel);
            t.setForeground(text);
            t.setGridColor(darkMode ? new Color(60, 63, 70) : new Color(230, 230, 230));
            t.setSelectionBackground(accent);
            t.setSelectionForeground(Color.WHITE);
        } else if (c instanceof JScrollPane sp) {
            sp.setBackground(panel);
            sp.getViewport().setBackground(panel);
        } else if (c instanceof JTabbedPane tp) {
            tp.setBackground(bg);
            tp.setForeground(text);
        }
    }

    private void styleTableHeader(JTableHeader header) {
        header.setFont(header.getFont().deriveFont(Font.BOLD));
    }

    // ------------------------------------------------------------------
    // Misc helpers
    // ------------------------------------------------------------------

    private String format(double v) {
        return String.format("%.2f", v);
    }

    private void replaceService(TradingService fresh) {
        // Copy the fresh state into the existing service so listeners keep working.
        service.getUser().setName(fresh.getUser().getName());
        service.getUser().setBalance(fresh.getUser().getBalance());
        service.getPortfolio().getHoldings().clear();
        service.getPortfolio().getHoldings().addAll(fresh.getPortfolio().getHoldings());
        service.getTransactions().clear();
        service.getTransactions().addAll(fresh.getTransactions());
        refreshAll();
    }

    // ------------------------------------------------------------------
    // Inner helpers
    // ------------------------------------------------------------------

    /**
     * Lightweight wrapper around JSplitPane to avoid an import dependency
     * on a single component in the main layout. Keeps the portfolio table
     * and graph side by side.
     */
    private static class JSplitPaneFix extends javax.swing.JSplitPane {
        JSplitPaneFix(Component left, Component right) {
            super(HORIZONTAL_SPLIT, left, right);
            setDividerLocation(0.65);
            setContinuousLayout(true);
        }
    }

    /**
     * Draws a simple line chart of the running profit/loss value so the
     * user can see the trend as the market simulates.
     */
    private class GraphPanel extends JPanel {
        private final java.util.List<Double> points = new java.util.ArrayList<>();
        private static final int MAX_POINTS = 60;

        void addPoint(double value) {
            points.add(value);
            if (points.size() > MAX_POINTS) {
                points.remove(0);
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            g2.setColor(darkMode ? new Color(40, 44, 52) : new Color(250, 250, 252));
            g2.fillRect(0, 0, w, h);

            // Zero baseline
            int midY = h / 2;
            g2.setColor(new Color(150, 150, 150));
            g2.drawLine(0, midY, w, midY);

            if (points.size() < 2) {
                g2.setColor(darkMode ? DARK_TEXT : LIGHT_TEXT);
                g2.drawString("Collecting data...", 10, 20);
                return;
            }

            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (double p : points) {
                if (p < min) min = p;
                if (p > max) max = p;
            }
            if (max - min < 1) {
                max = min + 1;
            }

            boolean positive = points.get(points.size() - 1) >= 0;
            g2.setColor(positive ? new Color(21, 128, 61) : new Color(185, 28, 28));

            int prevX = 0;
            int prevY = midY;
            int n = points.size();
            for (int i = 0; i < n; i++) {
                int x = (int) ((double) i / (n - 1) * w);
                double normalized = (points.get(i) - min) / (max - min);
                int y = h - (int) (normalized * h);
                if (i > 0) {
                    g2.drawLine(prevX, prevY, x, y);
                }
                prevX = x;
                prevY = y;
            }
        }
    }
}
