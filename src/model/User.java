package model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents an application user with a cash balance.
 * Encapsulates user identity and wallet balance.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final double INITIAL_BALANCE = 100000.0;

    private String name;
    private double balance;

    public User(String name) {
        this.name = name;
        this.balance = INITIAL_BALANCE;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return Objects.equals(name, user.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
