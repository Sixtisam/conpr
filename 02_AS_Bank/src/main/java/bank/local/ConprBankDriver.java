/*
 * Copyright (c) 2019 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package bank.local;

/* Simple Server -- NOW THREADSAFE */

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import bank.Account;
import bank.Bank;
import bank.InactiveException;
import bank.OverdrawException;

public class ConprBankDriver implements bank.BankDriver {
    private ConprBank bank = null;

    @Override
    public void connect(String[] args) {
        bank = new ConprBank();
    }

    @Override
    public void disconnect() {
        bank = null;
    }

    @Override
    public Bank getBank() {
        return bank;
    }
}

class ConprBank implements Bank {
    private final Object ACCOUNTS_LOCK = new Object();
    private final Object CLOSE_LOCK = new Object();
    private final Map<String, ConprAccount> accounts = new HashMap<>();

    @Override
    public Set<String> getAccountNumbers() throws IOException {
        synchronized (ACCOUNTS_LOCK) {
            return accounts.values().stream()
                    .filter(ConprAccount::isActive)
                    .map(ConprAccount::getNumber)
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public String createAccount(String owner) {
        final ConprAccount a = new ConprAccount(owner);
        synchronized (ACCOUNTS_LOCK) {
            accounts.put(a.getNumber(), a);
        }
        return a.getNumber();
    }

    @Override
    public boolean closeAccount(String number) {
        ConprAccount a;
        synchronized (ACCOUNTS_LOCK) {
            a = accounts.get(number);
        }

        if (a == null) {
            return false;
        }

        // prevent account being closed while a transfer is in process
        synchronized (CLOSE_LOCK) {
            synchronized (a) {
                if (a.getBalance() != 0 || !a.isActive()) {
                    return false;
                }
                a.passivate();
            }
            return true;
        }
    }

    @Override
    public Account getAccount(String number) {
        synchronized (ACCOUNTS_LOCK) {
            return accounts.get(number);
        }
    }

    @Override
    public void transfer(Account from0, Account to0, double amount)
            throws IOException, InactiveException, OverdrawException {
        ConprAccount from = (ConprAccount) from0;
        ConprAccount to = (ConprAccount) to0;

        // order in which to look accounts
        ConprAccount first, second;
        
        // in order to prevent a deadlock, a "global order" must be established
        if (from.getNumber().compareTo(to.getNumber()) == -1) {
            first = from;
            second = to;
        } else {
            first = to;
            second = from;
        }
        
        // prevent from and to from being closed and balance being changed through whole operation
        synchronized (first.activeLock) {
            synchronized (first.balanceLock) {
                synchronized (second.activeLock) {
                    synchronized (second.balanceLock) {
                        from.withdraw(amount);
                        try {
                            to.deposit(amount);
                        } catch (Exception e) {
                            from.deposit(amount);
                            throw e;
                        }
                    }
                }
            }

        }
    }
}

class ConprAccount implements Account {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(0);
    final Object activeLock = new Object();
    final Object balanceLock = new Object();
    private final String number;
    private final String owner;
    private double balance;
    private volatile boolean active = true;

    ConprAccount(String owner) {
        this.owner = owner;
        this.number = "CONPR_ACC_" + NEXT_ID.getAndIncrement();
    }

    @Override
    public String getOwner() {
        return owner;
    }

    @Override
    public String getNumber() {
        return number;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    void passivate() {
        synchronized (activeLock) {
            active = false;
        }
    }

    @Override
    public double getBalance() {
        synchronized (balanceLock) {
            return balance;
        }
    }

    @Override
    public void deposit(double amount) throws InactiveException {
        synchronized (activeLock) {
            synchronized (balanceLock) {
                if (!active)
                    throw new InactiveException("account not active");
                if (amount < 0)
                    throw new IllegalArgumentException("negative amount");
                balance += amount;
            }
        }
    }

    @Override
    public void withdraw(double amount) throws InactiveException, OverdrawException {
        synchronized (activeLock) {
            synchronized (balanceLock) {
                if (!active)
                    throw new InactiveException("account not active");
                if (amount < 0)
                    throw new IllegalArgumentException("negative amount");
                if (balance - amount < 0)
                    throw new OverdrawException("account cannot be overdrawn");
                balance -= amount;
            }
        }
    }

}
