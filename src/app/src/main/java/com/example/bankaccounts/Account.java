package com.example.bankaccounts;

public class Account {

    private int id;
    private String accountName;
    private double amount;
    private String iban;
    private String currency;

    public Account(int id, String accountName, double amount, String iban, String currency){
        this.id =id;
        this.accountName = accountName;
        this.amount=amount;
        this.iban=iban;
        this.currency=currency;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

    public String getAccountName() {
        return accountName;
    }
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public double getAmount() {
        return amount;
    }
    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getIban() {
        return iban;
    }
    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getCurrency() {
        return currency;
    }
    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
