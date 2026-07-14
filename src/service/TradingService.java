package service;

import model.Portfolio;
import model.Stock;
import model.Transaction;
import model.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates the business rules for buying and selling stock.
 * Validates balances and holdings, mutates the user/portfolio, and records
 * every trade in the transaction history.
 */
public class TradingService {

    private final User user;
    private final Portfolio portfolio;
    private final StockMarket market;
    private final List<Transaction> transactions = new ArrayList<>();

    public TradingService(User user, Portfolio portfolio, StockMarket market) {
        this.user = user;
        this.portfolio = portfolio;
        this.market = market;
    }

    public User getUser() {
        return user;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public StockMarket getMarket() {
        return market;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    /**
     * Buys the given quantity of a stock.
     *
     * @return null on success, an error message on failure.
     */
    public String buy(String symbol, int quantity) {
        if (quantity <= 0) {
            return "Quantity must be greater than zero.";
        }
        Stock stock = market.getStock(symbol);
        if (stock == null) {
            return "Unknown stock: " + symbol;
        }
        double cost = stock.getCurrentPrice() * quantity;
        if (cost > user.getBalance()) {
            return "Insufficient balance. Need ₹" + String.format("%.2f", cost)
                    + " but have ₹" + String.format("%.2f", user.getBalance());
        }

        user.setBalance(user.getBalance() - cost);
        portfolio.addShares(symbol, quantity, stock.getCurrentPrice());
        record(Transaction.Type.BUY, symbol, quantity, stock.getCurrentPrice());
        System.out.printf("Bought %d shares of %s @ ₹%.2f%n", quantity, symbol, stock.getCurrentPrice());
        return null;
    }

    /**
     * Sells the given quantity of an owned stock.
     *
     * @return null on success, an error message on failure.
     */
    public String sell(String symbol, int quantity) {
        if (quantity <= 0) {
            return "Quantity must be greater than zero.";
        }
        Stock stock = market.getStock(symbol);
        if (stock == null) {
            return "Unknown stock: " + symbol;
        }
        if (!portfolio.removeShares(symbol, quantity)) {
            return "Cannot sell " + quantity + " shares of " + symbol
                    + " (you don't own enough).";
        }

        double proceeds = stock.getCurrentPrice() * quantity;
        user.setBalance(user.getBalance() + proceeds);
        record(Transaction.Type.SELL, symbol, quantity, stock.getCurrentPrice());
        System.out.printf("Sold %d shares of %s @ ₹%.2f%n", quantity, symbol, stock.getCurrentPrice());
        return null;
    }

    private void record(Transaction.Type type, String symbol, int quantity, double price) {
        transactions.add(new Transaction(type, symbol, quantity, price, LocalDateTime.now()));
    }

    // ---- Report helpers -------------------------------------------------

    public double totalInvestment() {
        return portfolio.totalInvestment();
    }

    public double portfolioValue() {
        return portfolio.currentValue(sym -> {
            Stock s = market.getStock(sym);
            return (s != null) ? s.getCurrentPrice() : null;
        });
    }

    public double profitLoss() {
        return portfolio.profitLoss(sym -> {
            Stock s = market.getStock(sym);
            return (s != null) ? s.getCurrentPrice() : null;
        });
    }

    public double availableCash() {
        return user.getBalance();
    }
}
