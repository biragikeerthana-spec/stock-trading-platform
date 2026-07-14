package service;

import model.Stock;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Maintains the list of available stocks and simulates random price movement.
 * Holds the canonical stock reference so price changes propagate to every
 * component that reads from the market.
 */
public class StockMarket implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Maximum percentage a price can move in a single tick. */
    private static final double MAX_CHANGE_PERCENT = 0.05; // 5%
    private static final Random RANDOM = new Random();

    private final Map<String, Stock> stocks = new LinkedHashMap<>();

    /** Loads the 10 default stocks. */
    public void loadDefaultStocks() {
        addStock(new Stock("AAPL", "Apple Inc.", 220.50));
        addStock(new Stock("GOOGL", "Alphabet Inc.", 145.30));
        addStock(new Stock("MSFT", "Microsoft Corp.", 410.25));
        addStock(new Stock("AMZN", "Amazon.com Inc.", 178.90));
        addStock(new Stock("TSLA", "Tesla Inc.", 245.60));
        addStock(new Stock("META", "Meta Platforms Inc.", 495.10));
        addStock(new Stock("NFLX", "Netflix Inc.", 610.40));
        addStock(new Stock("NVDA", "NVIDIA Corp.", 880.75));
        addStock(new Stock("ORCL", "Oracle Corp.", 130.20));
        addStock(new Stock("IBM", "IBM Corp.", 195.85));
    }

    public void addStock(Stock stock) {
        stocks.put(stock.getSymbol(), stock);
    }

    public Stock getStock(String symbol) {
        return stocks.get(symbol);
    }

    public List<Stock> getAllStocks() {
        return new ArrayList<>(stocks.values());
    }

    /**
     * Applies a random price change to every stock in the market.
     * Returns the previous prices keyed by symbol so callers can show
     * "old price -> new price" in the UI.
     */
    public Map<String, Double> simulatePriceChanges() {
        Map<String, Double> oldPrices = new LinkedHashMap<>();
        for (Stock s : stocks.values()) {
            double oldPrice = s.getCurrentPrice();
            oldPrices.put(s.getSymbol(), oldPrice);

            // Random change between -MAX_CHANGE_PERCENT and +MAX_CHANGE_PERCENT
            double delta = (RANDOM.nextDouble() * 2 - 1) * MAX_CHANGE_PERCENT;
            double newPrice = oldPrice * (1 + delta);
            // Round to 2 decimals
            newPrice = Math.round(newPrice * 100.0) / 100.0;
            // Prevent non-positive prices
            if (newPrice <= 0) {
                newPrice = 0.01;
            }
            s.setCurrentPrice(newPrice);
        }
        return oldPrices;
    }

    /** Returns stocks sorted by current price (descending when desc=true). */
    public List<Stock> sortByPrice(boolean descending) {
        List<Stock> list = getAllStocks();
        list.sort(Comparator.comparingDouble(Stock::getCurrentPrice));
        if (descending) {
            java.util.Collections.reverse(list);
        }
        return list;
    }

    /** Returns stocks sorted by company name (ascending). */
    public List<Stock> sortByCompany() {
        List<Stock> list = getAllStocks();
        list.sort(Comparator.comparing(Stock::getCompanyName));
        return list;
    }

    /** Case-insensitive search by symbol or company name. */
    public List<Stock> search(String query) {
        List<Stock> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return getAllStocks();
        }
        String q = query.trim().toLowerCase();
        for (Stock s : stocks.values()) {
            if (s.getSymbol().toLowerCase().contains(q)
                    || s.getCompanyName().toLowerCase().contains(q)) {
                results.add(s);
            }
        }
        return results;
    }
}
