package model;

import java.io.Serializable;

/**
 * Represents a holding of a particular stock owned by the user.
 * Tracks quantity and the weighted average buy price.
 */
public class Holding implements Serializable {

    private static final long serialVersionUID = 1L;

    private String symbol;
    private int quantity;
    private double averageBuyPrice;

    public Holding(String symbol, int quantity, double averageBuyPrice) {
        this.symbol = symbol;
        this.quantity = quantity;
        this.averageBuyPrice = averageBuyPrice;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getAverageBuyPrice() {
        return averageBuyPrice;
    }

    public void setAverageBuyPrice(double averageBuyPrice) {
        this.averageBuyPrice = averageBuyPrice;
    }
}
