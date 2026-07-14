package model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable record of a single buy or sell trade.
 * Used for the transaction history table and CSV export.
 */
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum Type {
        BUY,
        SELL
    }

    private final Type type;
    private final String symbol;
    private final int quantity;
    private final double price;
    private final LocalDateTime dateTime;

    public Transaction(Type type, String symbol, int quantity, double price, LocalDateTime dateTime) {
        this.type = type;
        this.symbol = symbol;
        this.quantity = quantity;
        this.price = price;
        this.dateTime = dateTime;
    }

    public Type getType() {
        return type;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    /** Formatted timestamp suitable for table display. */
    public String getFormattedDateTime() {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Override
    public String toString() {
        return String.format("%s %d %s @ ₹%.2f (%s)", type, quantity, symbol, price, getFormattedDateTime());
    }
}
