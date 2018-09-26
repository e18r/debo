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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.lang.Exception;
import java.math.BigDecimal;


// TODO (critical): Put closing statements in finally blocks to tackle DoS attacks

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
	    file.close();
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
     * Creates a new user
     */
    public void newUser(String email, String sessionToken) throws Exception {
	String query = "INSERT INTO users (email, session_token) VALUES (?, ?)";
	PreparedStatement st = conn.prepareStatement(query);
	st.setString(1, email);
	st.setString(2, sessionToken);
	int rowsInserted = st.executeUpdate();
	if(rowsInserted != 1) {
	    throw new Exception("There was an error creating the user.");
	}
	st.close();
    }

    /**
     * Retrieves a session token
     */
    public String getSessionToken(String email) throws Exception {
	String sessionToken;
	String query = "SELECT session_token FROM users WHERE email = ?";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setString(1, email);
	    ResultSet rs = st.executeQuery();
	    if(rs.next()) {
		sessionToken = rs.getString(1);
	    }
	    else {
		throw new Exception("This user is not registered.");
	    }
	    rs.close();
	    st.close();
	}
	catch(SQLException e) {
	    System.out.println(e);
	    return null;
	}
	return sessionToken;
    }

    /**
     * Authenticates a user
     */
    public int authenticate(String sessionToken) throws Exception {
	String query = "SELECT id FROM users WHERE session_token = ?";
	PreparedStatement st = conn.prepareStatement(query);
	st.setString(1, sessionToken);
	ResultSet rs = st.executeQuery();
	if(rs.next()) {
	    int userId = rs.getInt(1);
	    rs.close();
	    st.close();
	    return userId;
	}
	else {
	    rs.close();
	    st.close();
	    throw new Exception("User not authenticated.");
	}
    }

    /**
     * Returns a list of currency types
     */
    public ArrayList<CurrencyType> getCurrencyTypes() {
	ArrayList<CurrencyType> cts = new ArrayList<CurrencyType>();
	String query = "SELECT id, name FROM currency_types";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    ResultSet rs = st.executeQuery();
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
	    PreparedStatement st = conn.prepareStatement(query);
	    ResultSet rs = st.executeQuery();
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
    public int findCurrencyId(String code, int userId) throws Exception {
	ArrayList<Currency> currencies = getCurrencies(new Currency(), userId);
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
    public int findAccountId(String name, int userId) throws Exception {
	ArrayList<Account> accounts = getAccounts(new Account(), userId);
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
    public String postCurrencies(Currency c, int userId) throws Exception, SQLException {
	int type = findCurrencyTypeId(c.type);
	String query = "INSERT INTO currencies (user_id, code, name, type) "
	    + "VALUES (?, ?, ?, ?) RETURNING code";
	PreparedStatement st = conn.prepareStatement(query);
	st.setInt(1, userId);
	st.setString(2, c.code);
	st.setString(3, c.name);
	st.setInt(4, type);
	st.execute();
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
    public String postAccounts(Account a, int userId) throws Exception, SQLException {       
	int typeId = findAccountTypeId(a.type);
	String query = "INSERT INTO accounts (user_id, name, type) "
	    + "VALUES (?, ?, ?) RETURNING name";
	PreparedStatement st = conn.prepareStatement(query);
	st.setInt(1, userId);
	st.setString(2, a.name);
	st.setInt(3, typeId);
	st.execute();
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
    public int postTransactions(Transaction t, int userId) throws Exception, SQLException {
	int currencyId = findCurrencyId(t.currency, userId);
	int debitId = findAccountId(t.debit, userId);
	int creditId = findAccountId(t.credit, userId);
	int optionalFields = 0;
	String query = "INSERT INTO transactions (user_id, ";
	if(t.date != null) {
	    query += "date, ";
	    optionalFields ++;
	}
	query += "amount, currency, debit, credit";
	if(t.comment != null) {
	    query += ", comment";
	    optionalFields ++;
	}
	query += ") VALUES (?, ?, ?, ?, ?";
	for(int i = 0; i < optionalFields; i ++) {
	    query += ", ?";
	}
	query += ") RETURNING id";
	PreparedStatement st = conn.prepareStatement(query);
	int offset = 0;
	st.setInt(1, userId);
	if(t.date != null) {
	    st.setString(2, t.date);
	    offset = 1;
	}
	st.setBigDecimal(2 + offset, t.amount);
	st.setInt(3 + offset, currencyId);
	st.setInt(4 + offset, debitId);
	st.setInt(5 + offset, creditId);
	if(t.comment != null) {
	    st.setString(6 + offset, t.comment);
	}
	st.execute();
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
    public ArrayList<Currency> getCurrencies(Currency filter, int userId) {
	ArrayList<Currency> currencies = new ArrayList<Currency>();
	String query = "SELECT currencies.id, code, currencies.name, "
	    + "currency_types.name "
	    + "FROM currencies "
	    + "JOIN currency_types ON currencies.type = currency_types.id "
	    + "WHERE user_id = ?";
	if(filter.type != null) {
	    query += " AND currency_types.name = ?";
	}
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setInt(1, userId);
	    if(filter.type != null) {
		st.setString(2, filter.type);
	    }
	    ResultSet rs = st.executeQuery();
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
    public ArrayList<Account> getAccounts(Account filter, int userId) {
	ArrayList<Account> accounts = new ArrayList<Account>();
	String query = "SELECT accounts.id, account_types.name, accounts.name "
	    + "FROM accounts "
	    + "JOIN account_types ON accounts.type = account_types.id "
	    + "WHERE accounts.user_id = ?";
	if(filter.type != null) {
	    query += " AND account_types.name = ?";
	}
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setInt(1, userId);
	    if(filter.type != null) {
		st.setString(2, filter.type);
	    }
	    ResultSet rs = st.executeQuery();
	    while(rs.next()) {
		Account a = new Account();
		a.id = rs.getInt(1);
		a.type = rs.getString(2);
		a.name = rs.getString(3);
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
    public ArrayList<Transaction> getTransactions(TxFilter f, int userId) {
	ArrayList<Transaction> transactions = new ArrayList<Transaction>();
	ArrayList<Object> values = new ArrayList<Object>();
	String query = "SELECT t.id, t.date, t.amount, c.code, a_debit.name, a_credit.name, "
	    + "t.comment "
	    + "FROM transactions t "
	    + "JOIN accounts a_debit ON t.debit = a_debit.id "
	    + "JOIN accounts a_credit ON t.credit = a_credit.id "
	    + "JOIN currencies c ON t.currency = c.id "
	    + "WHERE t.user_id = ?";
	values.add(userId);
	if(f.minDate != null || f.maxDate != null || f.minAmount != null || f.maxAmount != null
	   || f.currency != null || f.debit != null || f.credit != null || f.account != null
	   || f.commentHas != null) {
	    boolean firstCondition = true;
	    query += " AND ";
	    if(f.minDate != null) {
		firstCondition = false;
		query += "date > ?";
		values.add(f.minDate);
	    }
	    if(f.maxDate != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "date < ?";
		values.add(f.maxDate);
	    }
	    if(f.minAmount != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "amount > ?";
		values.add(f.minAmount);
	    }
	    if(f.maxAmount != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "amount < ?";
		values.add(f.maxAmount);
	    }
	    if(f.currency != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "c.code = ?";
		values.add(f.currency);
	    }
	    if(f.debit != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "a_debit.name = ?";
		values.add(f.debit);
	    }
	    if(f.credit != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "a_credit.name = ?";
		values.add(f.credit);
	    }
	    if(f.account != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "(a_debit.name = ? OR a_credit.name = ?)";
		values.add(f.account);
		values.add(f.account);
	    }
	    if(f.commentHas != null) {
		String pattern = f.commentHas.replace(" ", ".*");
		if(!firstCondition) {
		    query += " AND ";
		}
		query += "comment ~* ?";
		values.add(pattern);
	    }
	}
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    for(int i=0; i < values.size(); i++) {
		st.setObject(i+1, values.get(i));
	    }
	    ResultSet rs = st.executeQuery();
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
    public Currency getCurrency(String code, int userId) {
	Currency c = new Currency();
	String query = "SELECT currencies.id, code, currencies.name, "
	    + "currency_types.name "
	    + "FROM currencies "
	    + "JOIN currency_types ON currencies.type = currency_types.id "
	    + "WHERE user_id = ? AND code = ?";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setInt(1, userId);
	    st.setString(2, code);
	    ResultSet rs = st.executeQuery();
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
    public Account getAccount(String name, int userId) {
	Account a = new Account();
	String query = "SELECT accounts.id, account_types.name, accounts.name "
	    + "FROM accounts "
	    + "JOIN account_types ON accounts.type = account_types.id "
	    + "WHERE accounts.user_id = ? AND accounts.name = ?";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setInt(1, userId);
	    st.setString(2, name);
	    ResultSet rs = st.executeQuery();
	    if(rs.next()) {
		a.id = rs.getInt(1);
		a.type = rs.getString(2);
		a.name = rs.getString(3);
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
    public Transaction getTransaction(int id, int userId) {
	Transaction t = new Transaction();
	String query = "SELECT t.id, t.date, t.amount, c.code, a_debit.name, "
	    + "a_credit.name, t.comment "
	    + "FROM transactions t "
	    + "JOIN accounts a_debit ON t.debit = a_debit.id "
	    + "JOIN accounts a_credit ON t.credit = a_credit.id "
	    + "JOIN currencies c ON t.currency = c.id "
	    + "WHERE t.user_id = ? AND t.id = ?";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setInt(1, userId);
	    st.setInt(2, id);
	    ResultSet rs = st.executeQuery();
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
    public String patchCurrency(String oldCode, Currency c, int userId) throws Exception, SQLException {
	ArrayList<Object> values = new ArrayList<Object>();
	String query = "UPDATE currencies SET ";
	boolean firstStatement = true;
	if(c.code != null) {
	    firstStatement = false;
	    query += "code = ?";
	    values.add(c.code);
	}
	if(c.name != null) {
	    if(!firstStatement) {
		query += ", ";
	    }
	    else {
		firstStatement = false;
	    }
	    query += "name = ?";
	    values.add(c.name);
	}
	if(c.type != null) {
	    if(!firstStatement) {
		query += ", ";
	    }
	    int typeId = findCurrencyTypeId(c.type);
	    query += "type = ?";
	    values.add(typeId);
	}
	query += " WHERE user_id = ? AND code = ? RETURNING code";
	values.add(userId);
	values.add(oldCode);
	PreparedStatement st = conn.prepareStatement(query);
	for(int i=0; i < values.size(); i++) {
	    st.setObject(i+1, values.get(i));
	}
	st.execute();
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
    public String patchAccount(String oldName, Account a, int userId) throws Exception, SQLException {
	ArrayList<Object> values = new ArrayList<Object>();
	String query = "UPDATE accounts SET ";
	boolean firstStatement = true;
	if(a.type != null) {
	    firstStatement = false;
	    int typeId = findAccountTypeId(a.type);
	    query += "type = ?";
	    values.add(typeId);
	}
	if(a.name != null) {
	    if(!firstStatement) {
		query += ", ";
	    }
	    query += "name = ?";
	    values.add(a.name);
	}
	query += " WHERE user_id = ? AND name = ? RETURNING name";
	values.add(userId);
	values.add(oldName);
	PreparedStatement st = conn.prepareStatement(query);
	for(int i=0; i < values.size(); i++) {
	    st.setObject(i+1, values.get(i));
	}
	st.execute();
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
     * Updates a transaction
     */
    public void patchTransaction(int id, Transaction t, int userId) throws Exception, SQLException {
	ArrayList<Object> values = new ArrayList<Object>();
	String query = "UPDATE transactions SET ";
	boolean firstStatement = true;
	if(t.date != null) {
	    firstStatement = false;
	    query += "date = ?";
	    values.add(t.date);
	}
	if(t.amount != null) {
	    if(!firstStatement) {
		query += ", ";
	    }
	    else {
		firstStatement = false;
	    }
	    query += "amount = ?";
	    values.add(t.amount);
	}
	if(t.currency != null) {
	    if(!firstStatement) {
		query += ", ";
	    }
	    else {
		firstStatement = false;
	    }
	    int currencyId = findCurrencyId(t.currency, userId);
	    query += "currency = ?";
	    values.add(currencyId);
	}
	if(t.debit != null) {
	    if(!firstStatement) {
		query += ", ";
	    }
	    else {
		firstStatement = false;
	    }
	    int debitId = findAccountId(t.debit, userId);
	    query += "debit = ?";
	    values.add(debitId);
	}
	if(t.credit != null) {
	    if(!firstStatement) {
		query += ", ";
	    }
	    else {
		firstStatement = false;
	    }
	    int creditId = findAccountId(t.credit, userId);
	    query += "credit = ?";
	    values.add(creditId);
	}
	if(t.comment != null) {
	    if(!firstStatement) {
		query += ", ";
	    }
	    query += "comment = ?";
	    values.add(t.comment);
	}
	query += " WHERE user_id = ? AND id = ?";
	values.add(userId);
	values.add(id);
	PreparedStatement st = conn.prepareStatement(query);
	for(int i=0; i < values.size(); i++) {
	    st.setObject(i+1, values.get(i));
	}
	int rowsUpdated = st.executeUpdate();
	st.close();
	if(rowsUpdated == 0) {
	    throw new Exception("Transaction id not found.");
	}
    }

    /**
     * Deletes a currency
     */
    public void deleteCurrency(String code, int userId) throws Exception, SQLException {
	String query = "DELETE FROM currencies WHERE user_id = ? AND code = ?";
	PreparedStatement st = conn.prepareStatement(query);
	st.setInt(1, userId);
	st.setString(2, code);
	int rowsDeleted = st.executeUpdate();
	st.close();
	if(rowsDeleted == 0) {
	    throw new Exception("Currency code not found.");
	}
    }

    /**
     * Deletes an account
     */
    public void deleteAccount(String name, int userId) throws Exception, SQLException {
	String query = "DELETE FROM accounts WHERE user_id = ? AND name = ?";
	PreparedStatement st = conn.prepareStatement(query);
	st.setInt(1, userId);
	st.setString(2, name);
	int rowsDeleted = st.executeUpdate();
	st.close();
	if(rowsDeleted == 0) {
	    throw new Exception("Account name not found.");
	}
    }

    /**
     * Deletes a transaction
     */
    public void deleteTransaction(int id, int userId) throws Exception, SQLException {
	String query = "DELETE FROM transactions WHERE user_id = ? AND id = ?";
	PreparedStatement st = conn.prepareStatement(query);
	st.setInt(1, userId);
	st.setInt(2, id);
	int rowsDeleted = st.executeUpdate();
	st.close();
	if(rowsDeleted == 0) {
	    throw new Exception("Transaction id not found.");
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
	Account() {}
	public String toString() {
	    return "<" + type + "> " + name;
	}
    }

    public static class TxFilter {
	public String minDate;
	public String maxDate;
	public BigDecimal minAmount;
	public BigDecimal maxAmount;
	public String currency;
	public String debit;
	public String credit;
	public String account;
	public String commentHas;
	TxFilter() {}
	public String toString() {
	    return "\"" + commentHas + "\" (" + debit + ":" + credit + ") " + currency + " $" + minAmount + " - " + maxAmount + "$ <" + minDate + " - " + maxDate + ">";
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
