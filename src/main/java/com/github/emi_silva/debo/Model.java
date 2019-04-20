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
import java.time.Instant;
import java.sql.Timestamp;
import java.util.HashMap;

// TODO (critical): Put closing statements in finally blocks to tackle DoS attacks

public class Model {

    Connection conn;
    Properties props;

    /**
     * Initializes the database connection
     */
    public Model() throws FileNotFoundException, IOException, NullPointerException,
			  SQLException {
	props = readProps("/db.properties");
	String url = props.getProperty("url");
	conn = DriverManager.getConnection(url, props);
    }

    /**
     * Reads properties from a file, using a defaults file as fallback
     * @param relPath the relative path of the file starting from src/main/resources
     * @return the Properties object
     */
    Properties readProps(String relPath) throws FileNotFoundException, IOException,
						NullPointerException {
	String defaultExt = ".default";
	Properties props = new Properties();
	URL defaultLocation = this.getClass().getResource(relPath + defaultExt);
	URL location = this.getClass().getResource(relPath);
	FileReader defaultFile = new FileReader(defaultLocation.getPath());
	props.load(defaultFile);
	defaultFile.close();
	FileReader file = new FileReader(location.getPath());
	props.load(file);
	file.close();
	return props;
    }

    /**
     * Create a new token for a user. If the user doesn't exist, insert it.
     */
    public HashMap<String, String> newToken(String email, String sessionToken,
					    Instant tokenExpires) throws DeboException {
	HashMap<String, String> session = new HashMap<String, String>();
	String query = "INSERT INTO users (email, session_token, token_expires) VALUES (?, ?, ?) "
	    + "ON CONFLICT (email) DO UPDATE SET session_token = ?, token_expires = ? "
	    + "RETURNING session_token, token_expires";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setString(1, email);
	    st.setString(2, sessionToken);
	    st.setTimestamp(3, Timestamp.from(tokenExpires));
	    st.setString(4, sessionToken);
	    st.setTimestamp(5, Timestamp.from(tokenExpires));
	    st.execute();
	    ResultSet rs = st.getResultSet();
	    if(rs.next()) {
		session.put("session_token", rs.getString(1));
		session.put("token_expires", rs.getTimestamp(2).toInstant().toString());
	    }
	    else {
		throw new DeboException(500, "A database error occurred.");
	    }
	    st.close();
	}
	catch(SQLException e) {
	    throw new DeboException(500, "A database error occurred.");
	}
	return session;
    }

    /**
     * Retrieves a session token and expiration date
     */
    public HashMap<String, String> getSession(String email) throws DeboException {
	HashMap<String, String> session = new HashMap<String, String>();
	Instant tokenExpires;
	String query = "SELECT session_token, token_expires FROM users WHERE email = ?";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setString(1, email);
	    ResultSet rs = st.executeQuery();
	    if(rs.next()) {
		session.put("session_token", rs.getString(1));
		tokenExpires = rs.getTimestamp(2).toInstant();
		session.put("token_expires", tokenExpires.toString());
	    }
	    else {
		throw new DeboException(412, "Please create a user.");
	    }
	    rs.close();
	    st.close();
	}
	catch(SQLException e) {
	    throw new DeboException(500, "A database error occurred.");
	}
	if(Instant.now().isAfter(tokenExpires)) {
	    throw new DeboException(412, "Please create a token.");
	}
	else {
	    return session;
	}
    }

    /**
     * Checks whether a session token exists and hasn't expired
     */
    public int authenticate(String sessionToken) throws DeboException {
	String query = "SELECT id, token_expires FROM users WHERE session_token = ?";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setString(1, sessionToken);
	    ResultSet rs = st.executeQuery();
	    if(rs.next()) {
		int userId = rs.getInt(1);
		Instant tokenExpires = rs.getTimestamp(2).toInstant();
		rs.close();
		st.close();
		if(Instant.now().isAfter(tokenExpires)) {
		    throw new DeboException(401, "The session expired.");
		}
		return userId;
	    }
	    else {
		rs.close();
		st.close();
		throw new DeboException(401, "Invalid session token.");
	    }
	}
	catch(SQLException e) {
	    throw new DeboException(500, "A database error occurred.");
	}
    }

    /**
     * Invalidates a token
     */
    public void logout(int userId) throws DeboException {
	String query = "UPDATE users SET token_expires = ? WHERE id = ?";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setTimestamp(1, Timestamp.from(Instant.now()));
	    st.setInt(2, userId);
	    int rowsUpdated = st.executeUpdate();
	}
	catch(SQLException e) {
	    throw new DeboException(500, "A database error occurred.");
	}
    }

    /**
     * Returns a list of currency types
     */
    public ArrayList<CurrencyType> getCurrencyTypes() throws DeboException {
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
	    throw new DeboException(500, "A database error occurred.");
	}
	return cts;
    }

    /**
     * Returns a list of account types
     */
    public ArrayList<AccountType> getAccountTypes() throws DeboException {
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
	    throw new DeboException(500, "A database error occurred.");
	}
	return ats;
    }

    /**
     * Finds a currency type id given its name
     */
    public int findCurrencyTypeId(String type) throws DeboException {
	ArrayList<CurrencyType> currencyTypes = getCurrencyTypes();
	ArrayList<String> names = new ArrayList<String>();
	for(CurrencyType ct : currencyTypes) {
	    names.add(ct.name);
	    if(ct.name.equals(type)) {
		return ct.id;
	    }
	}
	throw new DeboException(400, "Valid types are: " + String.join(", ", names) + ".");
    }

    /**
     * Finds a currency id given its code
     */
    public int findCurrencyId(String code, int userId) throws DeboException {
	ArrayList<Currency> currencies = getCurrencies(new Currency(), userId);
	for(Currency c : currencies) {
	    if(c.code.equals(code)) {
		return c.id;
	    }
	}
	throw new DeboException(400, "Currency '" + code + "' doesn't exist.");
    }

    /**
     * Finds an account type id given its name
     */
    public int findAccountTypeId(String type) throws DeboException {
	ArrayList<AccountType> ats = getAccountTypes();
	ArrayList<String> names = new ArrayList<String>();
	for(AccountType at : ats) {
	    names.add(at.name);
	    if(at.name.equals(type)) {
		return at.id;
	    }
	}
	throw new DeboException(400, "Valid types are: " + String.join(", ", names) + ".");
    }

    /**
     * Finds an account id given its name
     */
    public int findAccountId(String name, int userId) throws DeboException {
	ArrayList<Account> accounts = getAccounts(new Account(), userId);
	for(Account a : accounts) {
	    if(a.name.equals(name)) {
		return a.id;
	    }
	}
	throw new DeboException(400, "Account '" + name + "' doesn't exist.");
    }

    /**
     * Creates a currency
     */
    public String postCurrencies(Currency c, int userId) throws DeboException {
	String code;
	String query = "INSERT INTO currencies (user_id, code, name, type) "
	    + "VALUES (?, ?, ?, ?) RETURNING code";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setInt(1, userId);
	    st.setString(2, c.code);
	    st.setString(3, c.name);
	    int type = findCurrencyTypeId(c.type);
	    st.setInt(4, type);
	    st.execute();
	    ResultSet rs = st.getResultSet();
	    rs.next();
	    code = rs.getString(1);
	    rs.close();
	    st.close();
	}
	catch(SQLException e) {
	    String currUniqueCnst = props.getProperty("currUniqueCnst");
	    if(e.getMessage().contains(currUniqueCnst)) {
		throw new DeboException(400, "A currency with code '" + c.code + "' already "
					+ "exists.");
	    }
	    throw new DeboException(500, "A database error occurred.");
	}
	return code;
    }
    
    /**
     * Creates an account
     */
    public String postAccounts(Account a, int userId) throws DeboException {
	String name;
	String query = "INSERT INTO accounts (user_id, name, type) "
	    + "VALUES (?, ?, ?) RETURNING name";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setInt(1, userId);
	    st.setString(2, a.name);
	    int typeId = findAccountTypeId(a.type);
	    st.setInt(3, typeId);
	    st.execute();
	    ResultSet rs = st.getResultSet();
	    rs.next();
	    name = rs.getString(1);
	    rs.close();
	    st.close();
	}
	catch(SQLException e) {
	    String accUniqueCnst = props.getProperty("accUniqueCnst");
	    if(e.getMessage().contains(accUniqueCnst)) {
		throw new DeboException(400, "An account with name '" + a.name +  "' already "
					+ "exists.");
	    }
	    throw new DeboException(500, "A database error occurred.");
	}
	return name;
    }

    /**
     * Creates a transaction
     */
    public int postTransactions(Transaction t, int userId) throws DeboException {
	int id;
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
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    int offset = 0;
	    st.setInt(1, userId);
	    if(t.date != null) {
		st.setString(2, t.date);
		offset = 1;
	    }
	    st.setBigDecimal(2 + offset, t.amount);
	    int currencyId = findCurrencyId(t.currency, userId);
	    st.setInt(3 + offset, currencyId);
	    int debitId = findAccountId(t.debit, userId);
	    st.setInt(4 + offset, debitId);
	    int creditId = findAccountId(t.credit, userId);
	    st.setInt(5 + offset, creditId);
	    if(t.comment != null) {
		st.setString(6 + offset, t.comment);
	    }
	    st.execute();
	    ResultSet rs = st.getResultSet();
	    rs.next();
	    id = rs.getInt(1);
	    rs.close();
	    st.close();
	}
	catch(SQLException e) {
	    String txDifferentCnst = props.getProperty("txDifferentCnst");
	    if(e.getMessage().contains(txDifferentCnst)) {
		throw new DeboException(400, "The debit and credit accounts must be different.");
	    }
	    String txPositiveCnst = props.getProperty("txPositiveCnst");
	    if(e.getMessage().contains(txPositiveCnst)) {
		throw new DeboException(400, "The amount must be positive.");
	    }
	    String txTimestampMsg = props.getProperty("txTimestampMsg");
	    if(e.getMessage().contains(txTimestampMsg)) {
		throw new DeboException(400, "Unrecognizable date format. Use ISO 8601.");
	    }
	    throw new DeboException(500, "A database error occurred.");
	}
	return id;
    }

    /**
     * Returns a (filtered) list of currencies
     */
    public ArrayList<Currency> getCurrencies(Currency filter, int userId) throws DeboException {
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
	    throw new DeboException(500, "A database error occurred.");
	}
	return currencies;
    }

    /**
     * Returns a (filtered) list of accounts
     */
    public ArrayList<Account> getAccounts(Account filter, int userId) throws DeboException {
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
	    throw new DeboException(500, "A database error occurred.");
	}
	return accounts;
    }

    /**
     * Returns a (filtered) list of transactions
     */
    public ArrayList<Transaction> getTransactions(TxFilter f, int userId) throws DeboException {
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
		query += "date >= ?";
		values.add(f.minDate);
	    }
	    if(f.maxDate != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "date <= ?";
		values.add(f.maxDate);
	    }
	    if(f.minAmount != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "amount >= ?";
		values.add(f.minAmount);
	    }
	    if(f.maxAmount != null) {
		if(!firstCondition) {
		    query += " AND ";
		}
		else {
		    firstCondition = false;
		}
		query += "amount <= ?";
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
	    throw new DeboException(500, "A database error occurred.");
	}
	return transactions;
    }
    
    /**
     * Returns a single currency
     */
    public Currency getCurrency(String code, int userId) throws DeboException {
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
		throw new DeboException(404, "The requested currency doesn't exist.");
	    }
	    rs.close();
	    st.close();
	}
	catch (SQLException e) {
	    throw new DeboException(500, "A database error occurred.");
	}
	return c;
    }

    /**
     * Returns a single account
     */
    public Account getAccount(String name, int userId) throws DeboException {
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
		throw new DeboException(404, "The requested account doesn't exist.");
	    }
	    rs.close();
	    st.close();
	}
	catch (SQLException e) {
	    throw new DeboException(500, "A database error occurred.");
	}
	return a;
    }

    /**
     * Returns a single transaction
     */
    public Transaction getTransaction(int id, int userId) throws DeboException {
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
		throw new DeboException(404, "The requested transaction doesn't exist.");
	    }
	    rs.close();
	    st.close();
	}
	catch(SQLException e) {
	    throw new DeboException(500, "A database error occurred.");
	}
	return t;
    }

    /**
     * Updates a currency
     */
    public String patchCurrency(String oldCode, Currency c, int userId) throws DeboException {
	String newCode = null;
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
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    for(int i=0; i < values.size(); i++) {
		st.setObject(i+1, values.get(i));
	    }
	    st.execute();
	    ResultSet rs = st.getResultSet();
	    if(rs.next()) {
		newCode = rs.getString(1);
	    }
	    rs.close();
	    st.close();
	}
	catch(SQLException e) {
	    throw new DeboException(500, "A database error occurred.");
	}
	if(newCode == null) {
	    throw new DeboException(404, "The requested currency doesn't exist.");
	}
	return newCode;
    }

    /**
     * Updates an account
     */
    public String patchAccount(String oldName, Account a, int userId) throws DeboException {
	String newName = null;
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
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    for(int i=0; i < values.size(); i++) {
		st.setObject(i+1, values.get(i));
	    }
	    st.execute();
	    ResultSet rs = st.getResultSet();
	    if(rs.next()) {
		newName = rs.getString(1);
	    }
	    rs.close();
	    st.close();
	}
	catch(SQLException e) {
	    throw new DeboException(500, "A database error occurred.");
	}
	if(newName == null) {
	    throw new DeboException(404, "The requested account doesn't exist.");
	}
	return newName;
    }

    /**
     * Updates a transaction
     */
    public void patchTransaction(int id, Transaction t, int userId) throws DeboException {
	int rowsUpdated = 0;
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
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    for(int i=0; i < values.size(); i++) {
		st.setObject(i+1, values.get(i));
	    }
	    rowsUpdated = st.executeUpdate();
	    st.close();
	}
	catch(SQLException e) {
	    String txDifferentCnst = props.getProperty("txDifferentCnst");
	    if(e.getMessage().contains(txDifferentCnst)) {
		throw new DeboException(400, "The debit and credit accounts must be different.");
	    }
	    String txPositiveCnst = props.getProperty("txPositiveCnst");
	    if(e.getMessage().contains(txPositiveCnst)) {
		throw new DeboException(400, "The amount must be positive.");
	    }
	    String txTimestampMsg = props.getProperty("txTimestampMsg");
	    if(e.getMessage().contains(txTimestampMsg)) {
		throw new DeboException(400, "Unrecognizable date format. Use ISO 8601.");
	    }
	    throw new DeboException(500, "A database error occurred.");
	}
	if(rowsUpdated == 0) {
	    throw new DeboException(404, "The requested transaction doesn't exist.");
	}
    }

    /**
     * Deletes a currency
     */
    public void deleteCurrency(String code, int userId) throws DeboException {
	int rowsDeleted = 0;
	String query = "DELETE FROM currencies WHERE user_id = ? AND code = ?";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setInt(1, userId);
	    st.setString(2, code);
	    rowsDeleted = st.executeUpdate();
	    st.close();
	}
	catch(SQLException e) {
	    String foreignCnst = props.getProperty("foreignCnst");
	    if(e.getMessage().contains(foreignCnst)) {
		throw new DeboException(400, "There are transactions using this currency. Please "
					+ "remove them and try again.");
	    }
	    throw new DeboException(500, "A database error occurred.");
	}
	if(rowsDeleted == 0) {
	    throw new DeboException(400, "The requested currency doesn't exist.");
	}
    }

    /**
     * Deletes an account
     */
    public void deleteAccount(String name, int userId) throws DeboException {
	int rowsDeleted = 0;
	String query = "DELETE FROM accounts WHERE user_id = ? AND name = ?";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setInt(1, userId);
	    st.setString(2, name);
	    rowsDeleted = st.executeUpdate();
	    st.close();
	}
	catch(SQLException e) {
	    String foreignCnst = props.getProperty("foreignCnst");
	    if(e.getMessage().contains(foreignCnst)) {
		throw new DeboException(400, "There are transactions using this account. Please "
					+ "remove them and try again.");
	    }
	    throw new DeboException(500, "A database error occurred.");
	}
	if(rowsDeleted == 0) {
	    throw new DeboException(400, "The requested account doesn't exist.");
	}
    }

    /**
     * Deletes a transaction
     */
    public void deleteTransaction(int id, int userId) throws DeboException {
	int rowsDeleted = 0;
	String query = "DELETE FROM transactions WHERE user_id = ? AND id = ?";
	try {
	    PreparedStatement st = conn.prepareStatement(query);
	    st.setInt(1, userId);
	    st.setInt(2, id);
	    rowsDeleted = st.executeUpdate();
	    st.close();
	}
	catch(SQLException e) {
	    throw new DeboException(500, "A database error occurred.");
	}
	if(rowsDeleted == 0) {
	    throw new DeboException(400, "The requested transaction doesn't exist.");
	}
    }

    /**
     * Returns an account's normal balance (debit or credit)
     */
    public String getNB(Account account) throws DeboException {
	if(account.type.equals("asset") || account.type.equals("expense")) {
	    return "debit";
	}
	else if(account.type.equals("liability") || account.type.equals("equity")
		|| account.type.equals("income")) {
	    return "credit";
	}
	else {
	    throw new DeboException(500, "Invalid account type");
	}
    }

    /**
     * Returns an account's balance
     */
    public HashMap<Account, BigDecimal> getBalance(String accountName, int userId)
	throws DeboException {
	HashMap<Account, BigDecimal> result = new HashMap<Account, BigDecimal>();
	Account account = getAccount(accountName, userId);
	BigDecimal balance = new BigDecimal(0);
	TxFilter filter = new TxFilter();
	filter.account = accountName;
	ArrayList<Transaction> txs = getTransactions(filter, userId);
	for(Transaction tx : txs) {
	    if((tx.debit.equals(accountName) && getNB(account).equals("debit"))
	       || (tx.credit.equals(accountName) && getNB(account).equals("credit"))) {
		balance = balance.add(tx.amount);
	    }
	    else {
		balance = balance.subtract(tx.amount);
	    }
	}
	result.put(account, balance);
	return result;
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
	    return "\"" + commentHas + "\" (" + debit + ":" + credit + ") " + currency + " $"
		+ minAmount + " - " + maxAmount + "$ <" + minDate + " - " + maxDate + ">";
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
	    return id + ": " + debit + " < $" + String.valueOf(amount) + " " + currency + " > "
		+ credit + " @ " + date + " | " + comment;
	}
    }
}
