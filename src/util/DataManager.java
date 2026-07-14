package util;

import model.Portfolio;
import model.Transaction;
import model.User;
import service.StockMarket;
import service.TradingService;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 * Handles all file persistence for the application using Java serialization
 * for the binary state files and plain text for the CSV export.
 *
 * Files written:
 *   users.dat        - serialized User
 *   portfolio.dat    - serialized Portfolio
 *   transactions.dat - serialized List<Transaction>
 *
 * The StockMarket is always rebuilt with default stocks on startup because
 * prices are simulated and not persisted.
 */
public class DataManager {

    private static final String USERS_FILE = "users.dat";
    private static final String PORTFOLIO_FILE = "portfolio.dat";
    private static final String TRANSACTIONS_FILE = "transactions.dat";

    /**
     * Attempts to load a previously saved user. Returns null when no save
     * file exists or deserialization fails.
     */
    public static User loadUser() {
        return deserialize(USERS_FILE, User.class);
    }

    public static Portfolio loadPortfolio() {
        Portfolio p = deserialize(PORTFOLIO_FILE, Portfolio.class);
        return (p != null) ? p : new Portfolio();
    }

    @SuppressWarnings("unchecked")
    public static List<Transaction> loadTransactions() {
        List<Transaction> t = deserialize(TRANSACTIONS_FILE, List.class);
        return (t != null) ? t : new java.util.ArrayList<>();
    }

    public static void saveUser(User user) {
        serialize(USERS_FILE, user);
    }

    public static void savePortfolio(Portfolio portfolio) {
        serialize(PORTFOLIO_FILE, portfolio);
    }

    public static void saveTransactions(List<Transaction> transactions) {
        serialize(TRANSACTIONS_FILE, transactions);
    }

    /**
     * Convenience method that saves the full state of a TradingService.
     */
    public static void saveAll(TradingService service) {
        saveUser(service.getUser());
        savePortfolio(service.getPortfolio());
        saveTransactions(service.getTransactions());
    }

    /**
     * Builds a fresh TradingService: loads saved data when present, otherwise
     * seeds a default user and an empty portfolio. The market is always
     * freshly populated with the 10 default stocks.
     */
    public static TradingService loadInitialState() {
        StockMarket market = new StockMarket();
        market.loadDefaultStocks();

        User user = loadUser();
        Portfolio portfolio = loadPortfolio();
        List<Transaction> transactions = loadTransactions();

        TradingService service;
        if (user == null) {
            user = new User("Trader");
            portfolio = new Portfolio();
            service = new TradingService(user, portfolio, market);
        } else {
            service = new TradingService(user, portfolio, market);
            service.getTransactions().addAll(transactions);
        }
        return service;
    }

    // ---- Serialization helpers ----------------------------------------

    private static <T> void serialize(String fileName, T object) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName))) {
            out.writeObject(object);
        } catch (IOException e) {
            System.err.println("Failed to save " + fileName + ": " + e.getMessage());
        }
    }

    private static <T> T deserialize(String fileName, Class<T> type) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName))) {
            Object obj = in.readObject();
            return type.cast(obj);
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            // Missing or corrupt file - caller will use defaults.
            return null;
        }
    }

    // ---- CSV export ----------------------------------------------------

    /**
     * Writes the transaction history to a CSV file at the given path.
     *
     * @return the number of rows written (excluding the header).
     */
    public static int exportTransactionsCsv(List<Transaction> transactions, String filePath) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileOutputStream(filePath))) {
            writer.println("Type,Symbol,Quantity,Price,DateTime");
            int count = 0;
            for (Transaction t : transactions) {
                writer.printf("%s,%s,%d,%.2f,%s%n",
                        t.getType(),
                        t.getSymbol(),
                        t.getQuantity(),
                        t.getPrice(),
                        t.getFormattedDateTime());
                count++;
            }
            return count;
        }
    }
}
