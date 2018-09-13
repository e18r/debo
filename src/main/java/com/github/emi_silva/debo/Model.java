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
import java.math.BigDecimal;


// TODO: Sanitize queries to prevent SQL injection
// TODO: Put closing statements in finally blocks to tackle DoS attacks

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
	String query = "SELECT id, name FROM currency_types";
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
     * Returns a list of account types
     */
    public ArrayList<AccountType> getAccountTypes() {
	ArrayList<AccountType> ats = new ArrayList<AccountType>();
	String query = "SELECT id, name FROM account_types";
	try {
	    Statement st = conn.createStatement();
	    ResultSet rs = st.executeQuery(query);
	    while(rs.next()) {
		AccountType at = new AccountType();
		at.id = rs.getInt(1);
		at.name = rs.getString(2);
		ats.add(at);
	    }
	    rs.close();
	    st.close();
	}
	catch(SQLException e) {
	    System.out.println(e);
	}
	return ats;
    }

    /**
     * Finds a currency type id given its name
     */
    public int findCurrencyTypeId(String type) throws Exception {
	for(CurrencyType ct : getCurrencyTypes()) {
	    if(ct.name.equals(type)) {
		return ct.id;
	    }
	}
	throw new Exception("Currency type not found.");
    }

    /**
     * Finds a currency id given its code
     */
    public int findCurrencyId(String code) throws Exception {
	ArrayList<Currency> currencies = getCurrencies(new Currency());
	for(Currency c : currencies) {
	    if(c.code.equals(code)) {
		return c.id;
	    }
	}
	throw new Exception("Currency code not found.");
    }

    /**
     * Finds an account type id given its name
     */
    public int findAccountTypeId(String type) throws Exception {
	ArrayList<AccountType> ats = getAccountTypes();
	for(AccountType at : ats) {
	    if(at.name.equals(type)) {
		return at.id;
	    }
	}
	throw new Exception("Account type not found.");
    }

    /**
     * Finds an account id given its name
     */
    public int findAccountId(String name) throws Exception {
	ArrayList<Account> accounts = getAccounts(new Account());
	for(Account a : accounts) {
	    if(a.name.equals(name)) {
		return a.id;
	    }
	}
	throw new Exception("Account id not found.");
    }

    /**
     * Creates a currency
     */
    public String postCurrencies(Currency c) throws Exception, SQLException {
	int type = findCurrencyTypeId(c.type);
	String query = "INSERT INTO currencies (code, name, type) "
	    + "VALUES ('" + c.code + "', '" + c.name + "', " + type + ")"
	    + "RETURNING code";
	Statement st = conn.createStatement();
	st.execute(query);
	ResultSet rs = st.getResultSet();
	rs.next();
	String code = rs.getString(1);
	rs.close();
	st.close();
	return code;
    }
    
    /**
     * Creates an account
     */
    public String postAccounts(Account a) throws Exception, SQLException {
	int currencyId = findCurrencyId(a.currency);
	int typeId = findAccountTypeId(a.type);
	if(a.name == null) {
	    throw new Exception("Please specify an account name.");
	}
	String query = "INSERT INTO accounts (name, currency, type) "
	    + "VALUES ('" + a.name + "', " + currencyId + ", " + typeId + ")"
	    + "RETURNING name";
	Statement st = conn.createStatement();
	st.execute(query);
	ResultSet rs = st.getResultSet();
	rs.next();
	String name = rs.getString(1);
	rs.close();
	st.close();
	return name;
    }

    /**
     * Creates a transaction
     */
    public int postTransactions(Transaction t) throws Exception, SQLException {
	int debitId = findAccountId(t.debit);
	int creditId = findAccountId(t.credit);
	String query = "INSERT INTO transactions (";
	if(t.date != null) {
	    query += "date, ";
	}
	query += "amount, debit, credit";
	if(t.comment != null) {
	    query += ", comment";
	}
	query += ") VALUES (";
	if(t.date != null) {
	    query += "TIMESTAMP WITH TIME ZONE '" + t.date + "', ";
	}
	query += t.amount.toPlainString() + ", " + debitId + ", " + creditId;
	if(t.comment != null) {
	    query += ", '" + t.comment + "'";
	}
	query += ") RETURNING id";
	Statement st = conn.createStatement();
	st.execute(query);
	ResultSet rs = st.getResultSet();
	rs.next();
	int id = rs.getInt(1);
	rs.close();
	st.close();
	return id;
    }

    /**
     * Returns a (filtered) list of currencies
     */
    public ArrayList<Currency> getCurrencies(Currency filter) {
	ArrayList<Currency> currencies = new ArrayList<Currency>();
	String query = "SELECT currencies.id, code, currencies.name, "
	    + "currency_types.name "
	    + "FROM currencies "
	    + "JOIN currency_types ON currencies.type = currency_types.id";
	if(filter.type != null) {
	    query += " WHERE currency_types.name = '" + filter.type + "'";
	}
	try {
	    Statement st = conn.createStatement();
	    ResultSet rs = st.executeQuery(query);
	    while(rs.next()) {
		Currency c = new Currency();
		c.id = rs.getInt(1);
		c.code = rs.getString(2);
		c.name = rs.getString(3);
		c.type = rs.getString(4);
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
     * Returns a (filtered) list of accounts
     */
    public ArrayList<Account> getAccounts(Account filter) {
	ArrayList<Account> accounts = new ArrayList<Account>();
	String query = "SELECT accounts.id, account_types.name, accounts.name, "
	    + "currencies.code "
	    + "FROM accounts "
	    + "JOIN account_types ON accounts.type = account_types.id "
	    + "JOIN currencies on accounts.currency = currencies.id";
	if(filter.type != null || filter.currency != null) {
	    boolean firstCondition = true;
	    query += " WHERE ";
	    if(filter.type != null) {
		firstCondition = false;
		query += "account_types.name = '" + filter.type + "'";
	    }
	    if(filter.currency != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		query += "currencies.code = '" + filter.currency + "'";
	    }
	}
	try {
	    Statement st = conn.createStatement();
	    ResultSet rs = st.executeQuery(query);
	    while(rs.next()) {
		Account a = new Account();
		a.id = rs.getInt(1);
		a.type = rs.getString(2);
		a.name = rs.getString(3);
		a.currency = rs.getString(4);
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

    /**
     * Returns a (filtered) list of transactions
     */
    public ArrayList<Transaction> getTransactions(TxFilter f) {
	ArrayList<Transaction> transactions = new ArrayList<Transaction>();
	String query = "SELECT t.id, t.date, t.amount, coalesce(c_debit.code, c_credit.code), "
	    + "a_debit.name, a_credit.name, t.comment "
	    + "FROM transactions t "
	    + "LEFT JOIN accounts a_debit ON t.debit = a_debit.id "
	    + "LEFT JOIN accounts a_credit ON t.credit = a_credit.id "
	    + "LEFT JOIN currencies c_debit ON a_debit.currency = c_debit.id "
	    + "LEFT JOIN currencies c_credit ON a_credit.currency = c_credit.id";
	if(f.minDate != null || f.maxDate != null || f.minAmount != null || f.maxAmount != null
	   || f.debit != null || f.credit != null || f.account != null || f.commentHas != null) {
	    boolean firstCondition = true;
	    query += " WHERE ";
	    if(f.minDate != null) {
		firstCondition = false;
		query += "date > '" + f.minDate + "'";
	    }
	    if(f.maxDate != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "date < '" + f.maxDate + "'";
	    }
	    if(f.minAmount != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "amount > " + f.minAmount;
	    }
	    if(f.maxAmount != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "amount < " + f.maxAmount;
	    }
	    if(f.debit != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "debit = '" + f.debit + "'";
	    }
	    if(f.credit != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "credit = '" + f.credit + "'";
	    }
	    if(f.account != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "(credit = '" + f.account + "' OR debit = '" + f.account + "')";
	    }
	    if(f.commentHas != null) {
		String pattern = f.commentHas.replace(" ", ".*");
		if(!firstCondition) {
		    query += " AND ";
		}
		query += "comment ~* '" + pattern + "'";
	    }
	}
	try {
	    Statement st = conn.createStatement();
	    ResultSet rs = st.executeQuery(query);
	    while(rs.next()) {
		Transaction t = new Transaction();
		t.id = rs.getInt(1);
		t.date = rs.getString(2);
		t.amount = rs.getBigDecimal(3);
		t.currency = rs.getString(4);
		t.debit = rs.getString(5);
		t.credit = rs.getString(6);
		t.comment = rs.getString(7);
		transactions.add(t);
	    }
	    rs.close();
	    st.close();
	}
	catch (SQLException e) {
	    System.out.println(e);
	}
	return transactions;
    }
    
    /**
     * Returns a single currency
     */
    public Currency getCurrency(String code) {
	Currency c = new Currency();
	String query = "SELECT currencies.id, code, currencies.name, "
	    + "currency_types.name "
	    + "FROM currencies "
	    + "JOIN currency_types ON currencies.type = currency_types.id "
	    + "WHERE code = '" + code + "'";
	try {
	    Statement st = conn.createStatement();
	    ResultSet rs = st.executeQuery(query);
	    if(rs.next()) {
		c.id = rs.getInt(1);
		c.code = rs.getString(2);
		c.name = rs.getString(3);
		c.type = rs.getString(4);
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
     * Returns a single account
     */
    public Account getAccount(String name) {
	Account a = new Account();
	String query = "SELECT accounts.id, account_types.name, accounts.name, "
	    + "currencies.code "
	    + "FROM accounts "
	    + "JOIN account_types ON accounts.type = account_types.id "
	    + "JOIN currencies ON accounts.currency = currencies.id "
	    + "WHERE accounts.name = '" + name + "'";
	try {
	    Statement st = conn.createStatement();
	    ResultSet rs = st.executeQuery(query);
	    if(rs.next()) {
		a.id = rs.getInt(1);
		a.type = rs.getString(2);
		a.name = rs.getString(3);
		a.currency = rs.getString(4);
	    }
	    else {
		a = null;
	    }
	    rs.close();
	    st.close();
	}
	catch (SQLException e) {
	    System.out.println(e);
	}
	return a;
    }

    /**
     * Returns a single transaction
     */
    public Transaction getTransaction(int id) {
	Transaction t = new Transaction();
	String query = "SELECT t.id, t.date, t.amount, coalesce(c_debit.code, c_credit.code), "
	    + "a_debit.name, a_credit.name, t.comment "
	    + "FROM transactions t "
	    + "LEFT JOIN accounts a_debit ON t.debit = a_debit.id "
	    + "LEFT JOIN accounts a_credit ON t.credit = a_credit.id "
	    + "LEFT JOIN currencies c_debit ON a_debit.currency = c_debit.id "
	    + "LEFT JOIN currencies c_credit ON a_credit.currency = c_credit.id "
	    + "WHERE t.id = " + id;
	try {
	    Statement st = conn.createStatement();
	    ResultSet rs = st.executeQuery(query);
	    if(rs.next()) {
		t.id = rs.getInt(1);
		t.date = rs.getString(2);
		t.amount = rs.getBigDecimal(3);
		t.currency = rs.getString(4);
		t.debit = rs.getString(5);
		t.credit = rs.getString(6);
		t.comment = rs.getString(7);
	    }
	    else {
		t = null;
	    }
	    rs.close();
	    st.close();
	}
	catch(SQLException e) {
	    System.out.println(e);
	}
	return t;
    }

    /**
     * Updates a currency
     */
    public String patchCurrency(String oldCode, Currency c) throws Exception, SQLException {
	String query = "UPDATE currencies SET ";
	boolean firstStatement = true;
	if(c.code != null) {
	    query += "code = '" + c.code + "'";
	    firstStatement = false;
	}
	if(c.name != null) {
	    if(!firstStatement) {
		query += ", ";
	    }
	    query += "name = '" + c.name + "'";
	    firstStatement = false;
	}
	if(c.type != null) {
	    if(!firstStatement) {
		query += ", ";
	    }
	    int typeId = findCurrencyTypeId(c.type);
	    query += "type = " + typeId;
	}
	query += " WHERE code = '" + oldCode + "' RETURNING code";
	Statement st = conn.createStatement();
	st.execute(query);
	ResultSet rs = st.getResultSet();
	String newCode = null;
	if(rs.next()) {
	    newCode = rs.getString(1);
	}
	rs.close();
	st.close();
	if(newCode == null) {
	    throw new Exception("Currency code not found.");
	}
	return newCode;
    }

    /**
     * Updates an account
     */
    public String patchAccount(String oldName, Account a) throws Exception, SQLException {
	String query = "UPDATE accounts SET ";
	boolean firstStatement = true;
	if(a.type != null) {
	    int typeId = findAccountTypeId(a.type);
	    query += "type = " + typeId;
	    firstStatement = false;
	}
	if(a.name != null) {
	    if(!firstStatement) {
		query += ", ";
	    }
	    query += "name = '" + a.name + "'";
	    firstStatement = false;
	}
	if(a.currency != null) {
	    if(!firstStatement) {
		query += ", ";
	    }
	    int currencyId = findCurrencyId(a.currency);
	    query += "currency = " + currencyId;	    
	}
	query += " WHERE name = '" + oldName + "' RETURNING name";
	Statement st = conn.createStatement();
	st.execute(query);
	ResultSet rs = st.getResultSet();
	String newName = null;
	if(rs.next()) {
	    newName = rs.getString(1);
	}
	rs.close();
	st.close();
	if(newName == null) {
	    throw new Exception("Account name not found.");
	}
	return newName;
    }

    /**
     * Deletes a currency
     */
    public void deleteCurrency(String code) throws Exception, SQLException {
	String query = "DELETE FROM currencies WHERE code = '" + code + "'";
	Statement st = conn.createStatement();
	int rowsDeleted = st.executeUpdate(query);
	st.close();
	if(rowsDeleted == 0) {
	    throw new Exception("Currency code not found.");
	}
    }

    /**
     * Deletes an account
     */
    public void deleteAccount(String name) throws Exception, SQLException {
	String query = "DELETE FROM accounts WHERE name = '" + name + "'";
	Statement st = conn.createStatement();
	int rowsDeleted = st.executeUpdate(query);
	st.close();
	if(rowsDeleted == 0) {
	    throw new Exception("Account name not found.");
	}
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
	public int id;
	public String code;
	public String name;
	public String type;
	Currency() {}
	public String toString() {
	    return code + ": " + name + " (" + type + ")";
	}
    }

    public static class AccountType {
	public int id;
	public String name;
	AccountType() {}
	public String toString() {
	    return name;
	}
    }

    public static class Account {
	public int id;
	public String type;
	public String name;
	public String currency;
	Account() {}
	public String toString() {
	    return "<" + type + "> " + name + " (" + currency + ")";
	}
    }

    public static class TxFilter {
	public String minDate;
	public String maxDate;
	public BigDecimal minAmount;
	public BigDecimal maxAmount;
	public String debit;
	public String credit;
	public String account;
	public String commentHas;
	TxFilter() {}
	public String toString() {
	    return "\"" + commentHas + "\" (" + debit + ":" + credit + ") $" + minAmount + " - " + maxAmount + "$ <" + minDate + " - " + maxDate + ">";
	}
    }

    public static class Transaction {
	public int id;
	public String date;
	public BigDecimal amount;
	public String currency;
	public String debit;
	public String credit;
	public String comment;
	Transaction() {}
	public String toString() {
	    return id + ": " + debit + " < $" + String.valueOf(amount) + " " + currency + " > " + credit + " @ " + date + " | " + comment;
	}
    }
}
