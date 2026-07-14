package model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a single stock in the market.
 * Encapsulates symbol, company name and current price.
 */
public class Stock implements Serializable {

    private static final long serialVersionUID = 1L;

    private String symbol;
    private String companyName;
    private double currentPrice;

    public Stock(String symbol, String companyName, double currentPrice) {
        this.symbol = symbol;
        this.companyName = companyName;
        this.currentPrice = currentPrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Stock)) return false;
        Stock stock = (Stock) o;
        return Objects.equals(symbol, stock.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) - ₹%.2f", symbol, companyName, currentPrice);
    }
}
