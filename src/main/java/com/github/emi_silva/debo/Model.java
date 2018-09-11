package com.github.emi_silva.debo;

import java.util.Properties;
import java.net.URL;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.sql.Statement;
import java.sql.ResultSet;
import java.lang.Exception;

public class Model {

    Connection conn;

    /**
     * Initializes the database connection
     */
    public Model() {
	String url = "jdbc:postgresql://localhost:5432/debo";
	Properties props = readProps("/db.properties");
	try {
	    conn = DriverManager.getConnection(url, props);
	}
	catch(SQLException e) {
	    System.out.println(e);
	}
    }

    /**
     * Reads properties from a file
     * @param relPath the relative path of the file starting from
     * src/main/resources
     * @return the Properties object
     */
    Properties readProps(String relPath) {
	Properties props = new Properties();
	URL location = this.getClass().getResource(relPath);
	try {
	    FileReader file = new FileReader(location.getPath());
	    props.load(file);
	}
	catch (FileNotFoundException e) {
	    System.out.println(e);
	}
	catch (IOException e) {
	    System.out.println(e);
	}
	return props;
    }

    /**
     * Returns a list of currency types
     */
    public ArrayList<CurrencyType> getCurrencyTypes() {
	ArrayList<CurrencyType> cts = new ArrayList<CurrencyType>();
	String query = "SELECT * FROM currency_types";
	try {
	    Statement st = conn.createStatement();
	    ResultSet rs = st.executeQuery(query);
	    while(rs.next()) {
		CurrencyType ct = new CurrencyType();
		ct.id = rs.getInt(1);
		ct.name = rs.getString(2);
		cts.add(ct);
	    }
	    rs.close();
	    st.close();
	}
	catch (SQLException e) {
	    System.out.println(e);
	}
	return cts;
    }

    /**
     * Returns a list of currencies
     */
    public ArrayList<Currency> getCurrencies() {
	ArrayList<Currency> currencies = new ArrayList<Currency>();
	String query = "SELECT code, currencies.name, currency_types.name "
	    + "FROM currencies "
	    + "JOIN currency_types ON currencies.type = currency_types.id";
	try {
	    Statement st = conn.createStatement();
	    ResultSet rs = st.executeQuery(query);
	    while(rs.next()) {
		Currency c = new Currency();
		c.code = rs.getString(1);
		c.name = rs.getString(2);
		c.type = rs.getString(3);
		currencies.add(c);
	    }
	    rs.close();
	    st.close();
	}
	catch (SQLException e) {
	    System.out.println(e);
	}
	return currencies;
    }

    /**
     * Returns a single currency
     */
    public Currency getCurrency(String code) {
	Currency c = new Currency();
	String query = "SELECT code, currencies.name, currency_types.name "
	    + "FROM currencies "
	    + "JOIN currency_types ON currencies.type = currency_types.id "
	    + "WHERE code = '" + code + "'";
	try {
	    Statement st = conn.createStatement();
	    ResultSet rs = st.executeQuery(query);
	    if(rs.next()) {
		c.code = rs.getString(1);
		c.name = rs.getString(2);
		c.type = rs.getString(3);
	    }
	    else {
		c = null;
	    }
	    rs.close();
	    st.close();
	}
	catch (SQLException e) {
	    System.out.println(e);
	}
	return c;
    }

    /**
     * Finds a currency type id given its name
     */
    public int findCurrencyType(String type) throws Exception {
	for(CurrencyType ct : getCurrencyTypes()) {
	    if(ct.name.equals(type)) {
		return ct.id;
	    }
	}
	throw new Exception("not found");
    }

    /**
     * Creates a new currency
     */
    public void postCurrencies(Currency c) throws Exception, SQLException {
	int type = findCurrencyType(c.type);
	String query = "INSERT INTO currencies (code, name, type) "
	    + "VALUES ('" + c.code + "', '" + c.name + "', '" + type + "')";
	Statement st = conn.createStatement();
	st.executeUpdate(query);
	st.close();
    }

    /**
     * Returns a human readable list of accounts
     */
    public ArrayList<Account> getAccounts() {
	ArrayList<Account> accounts = new ArrayList<Account>();
	String query = "SELECT account_types.name, accounts.name, code "
	    + "FROM accounts "
	    + "JOIN account_types ON accounts.type = account_types.id "
	    + "JOIN currencies on accounts.currency = currencies.id";
	try {
	    Statement st = conn.createStatement();
	    ResultSet rs = st.executeQuery(query);
	    while(rs.next()) {
		Account a = new Account();
		a.type = rs.getString(1);
		a.name = rs.getString(2);
		a.currency = rs.getString(3);
		accounts.add(a);
	    }
	    rs.close();
	    st.close();
	}
	catch (SQLException e) {
	    System.out.println(e);
	}
	return accounts;
    }

    public static class CurrencyType {
	public int id;
	public String name;
	CurrencyType() {}
	public String toString() {
	    return name;
	}
    }

    public static class Currency {
	public String code;
	public String name;
	public String type;
	Currency() {}
	public String toString() {
	    return code + ": " + name + " (" + type + ")";
	}
    }

    public static class Account {
	public String type;
	public String name;
	public String currency;
	Account() {}
	public String toString() {
	    return "<" + type + "> " + name + " (" + currency + ")";
	}
    }

}
