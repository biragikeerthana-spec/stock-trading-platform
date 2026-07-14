package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Collection of all the user's holdings. Provides buy/sell helpers and
 * aggregate valuation metrics used by the dashboard and reports.
 */
public class Portfolio implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Holding> holdings = new ArrayList<>();

    public List<Holding> getHoldings() {
        return holdings;
    }

    /** Finds the holding for a symbol, or null when not owned. */
    public Holding find(String symbol) {
        for (Holding h : holdings) {
            if (h.getSymbol().equals(symbol)) {
                return h;
            }
        }
        return null;
    }

    /**
     * Adds shares to a holding, recalculating the weighted average buy price.
     */
    public void addShares(String symbol, int quantity, double price) {
        Holding h = find(symbol);
        if (h == null) {
            holdings.add(new Holding(symbol, quantity, price));
        } else {
            int oldQty = h.getQuantity();
            double oldAvg = h.getAverageBuyPrice();
            double newAvg = ((oldQty * oldAvg) + (quantity * price)) / (oldQty + quantity);
            h.setAverageBuyPrice(newAvg);
            h.setQuantity(oldQty + quantity);
        }
    }

    /**
     * Removes shares from a holding. Removes the holding entirely when the
     * quantity reaches zero.
     *
     * @return true if the sale was applied, false if the user lacks shares.
     */
    public boolean removeShares(String symbol, int quantity) {
        Holding h = find(symbol);
        if (h == null || h.getQuantity() < quantity) {
            return false;
        }
        h.setQuantity(h.getQuantity() - quantity);
        if (h.getQuantity() == 0) {
            holdings.remove(h);
        }
        return true;
    }

    /** Total amount of money invested across all holdings. */
    public double totalInvestment() {
        double total = 0;
        for (Holding h : holdings) {
            total += h.getQuantity() * h.getAverageBuyPrice();
        }
        return total;
    }

    /**
     * Current market value of all holdings, using the supplied price lookup
     * function to resolve the live price for each symbol. Passing a function
     * (instead of a StockMarket reference) keeps the model layer free of any
     * dependency on the service layer.
     */
    public double currentValue(Function<String, Double> priceLookup) {
        double total = 0;
        for (Holding h : holdings) {
            Double price = priceLookup.apply(h.getSymbol());
            double p = (price != null) ? price : h.getAverageBuyPrice();
            total += h.getQuantity() * p;
        }
        return total;
    }

    /** Profit / loss = current value - total invested. */
    public double profitLoss(Function<String, Double> priceLookup) {
        return currentValue(priceLookup) - totalInvestment();
    }
}
