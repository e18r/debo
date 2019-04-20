package com.github.emi_silva.debo;

import java.util.Properties;
import java.security.SecureRandom;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.ArrayList;
import java.time.Instant;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.math.BigDecimal;

public class Logic {

    private Model model;
    private Properties authProps;
    private SecureRandom random;
    private static final char[] symbols = ("0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
					   + "abcdefghijklmnopqrstuvwxyz").toCharArray();

    public Logic() throws FileNotFoundException, IOException, NullPointerException,
			  SQLException {
	model = new Model();
	authProps = model.readProps("/auth.properties");
	byte[] seed = authProps.getProperty("seed").getBytes();
	random = new SecureRandom(seed);
    }

    /**
     * Builds the Google OAuth URI the client will be redirected to for authorization
     */
    public String getOAuthUri() {
	String OAuthEndpoint = authProps.getProperty("oauthEndpoint");
	String clientId = authProps.getProperty("clientId");
	String redirectUri = authProps.getProperty("redirectUri");
	String scope = authProps.getProperty("scope");
	String responseType = authProps.getProperty("responseType");
	String uri = OAuthEndpoint + "?client_id=" + clientId + "&redirect_uri=" + redirectUri
	    + "&scope=" + scope + "&response_type=" + responseType;
	return uri;
    }

    /**
     * Given a code provider by the authenticated user, gets an access token
     */
    public String getAccessToken(String code) throws DeboException {
	if(code == null) {
	    throw new DeboException(400, "Please provide a code.");
	}
	String accessTokenEndpoint = authProps.getProperty("accessTokenEndpoint");
	String contentHeader = authProps.getProperty("contentHeader");
	String clientId = authProps.getProperty("clientId");
	String clientSecret = authProps.getProperty("clientSecret");
	String redirectUri = authProps.getProperty("redirectUri");
	String grantType = authProps.getProperty("grantType");
	HttpResponse<JsonNode> response;
	try {
	    response = Unirest.post(accessTokenEndpoint)
		.header("Content-Type", contentHeader)
		.field("code", code)
		.field("client_id", clientId)
		.field("client_secret", clientSecret)
		.field("redirect_uri", redirectUri)
		.field("grantType", grantType)
		.asJson();
	}
	catch(UnirestException e) {
	    throw new DeboException(500, "A network error occurred.");
	}
	JSONObject body;
	try {
	    body = new JSONObject(response.getBody().toString());
	}
	catch(JSONException e) {
	    throw new DeboException(500, "A network error occurred.");
	}
	if(!body.has("access_token")) {
	    throw new DeboException(400, "Could not obtain an access token.");
	}
	String accessToken = body.getString("access_token");
	return accessToken;
    }

    /**
     * Gets the user's email given the access token
     */
    public String getEmail(String accessToken) throws DeboException {
	String APIEndpoint = authProps.getProperty("APIEndpoint");
	String alt = authProps.getProperty("alt");
	String URI = APIEndpoint + "?alt=" + alt;
	String authHeader = authProps.getProperty("authKeyword") + " " + accessToken;
	HttpResponse<JsonNode> response;
	try {
	response = Unirest.get(URI)
	    .header("Authorization", authHeader)
	    .asJson();
	}
	catch(UnirestException e) {
	    throw new DeboException(500, "A network error ocurred.");
	}
	JSONObject body;
	try {
	    body = new JSONObject(response.getBody().toString());
	}
	catch(JSONException e) {
	    throw new DeboException(500, "A network error occurred.");
	}
	if(!body.has("email")) {
	    throw new DeboException(500, "Could not obtain the user's email.");
	}
	String email = body.getString("email");
	return email;
    }

    /**
     * Generates a random string
     * Inspired by https://stackoverflow.com/a/41156/2430274
     */
    public String newRandomString(int length) {
	char[] token = new char[length];
	for(int i = 0; i < token.length; i++) {
	    token[i] = symbols[random.nextInt(symbols.length)];
	}
	return new String(token);
    }

    /**
     * Creates a new session token (and a user if they don't already exist)
     */
    private HashMap<String, String> newToken(String email) throws DeboException {
	int length = Integer.valueOf(authProps.getProperty("tokenLength"));
	String sessionToken = newRandomString(length);
	long lifetime = Long.valueOf(authProps.getProperty("tokenLifetime"));
	Instant tokenExpires = Instant.now().plusSeconds(lifetime);
	return model.newToken(email, sessionToken, tokenExpires);
    }

    /**
     * If the user exists and a valid session token exists, get the latter.
     * If the user exists but the session token is invalid, create a new one.
     * If the user doesn't exist, create them and a new session token.
     */
    public HashMap<String, String> getSession(String email) throws DeboException {
	HashMap<String, String> session;
	try {
	    return model.getSession(email);
	}
	catch(DeboException e) {
	    if(e.code == 412) {
		return newToken(email);
	    }
	    else {
		throw e;
	    }
	}
    }

    /**
     * Checks whether a request's credentials are valid
     */
    public int authenticate(String authHeader) throws DeboException {
	String[] authElems = authHeader.split(" ");
	String authKeyword = authElems[0];
	if(authElems.length < 2
	   || !authKeyword.equals(authProps.getProperty("authKeyword"))) {
	    throw new DeboException(401, "Invalid authorization header format.");
	}
	String sessionToken = authElems[1];
	int userId = model.authenticate(sessionToken);
	return userId;
    }

    /**
     * Logs a user out
     */
    public void logout(int userId) throws DeboException {
	model.logout(userId);
    }

    public ArrayList<Model.CurrencyType> getCurrencyTypes() throws DeboException {
	return model.getCurrencyTypes();
    }

    public ArrayList<Model.AccountType> getAccountTypes() throws DeboException {
	return model.getAccountTypes();
    }

    public Model.Currency postCurrencies(Model.Currency c, int userId) throws DeboException {
	if(c.code == null || c.name == null || c.type == null) {
	    throw new DeboException(400, "The following fields are required: code, name, type.");
	}
	if(c.code.length() != 3) {
	    throw new DeboException(400, "Currency codes must be three letters long.");
	}
	String newCode = model.postCurrencies(c, userId);
	Model.Currency newCurrency = model.getCurrency(newCode, userId);
	return newCurrency;
    }
    public Model.Account postAccounts(Model.Account a, int userId) throws DeboException {
	if(a.type == null || a.name == null) {
	    throw new DeboException(400, "The following fields are required: type, name.");
	}
	if(a.name.length() == 0) {
	    throw new DeboException(400, "Account names cannot be empty.");
	}
	String newName = model.postAccounts(a, userId);
	Model.Account newAccount = model.getAccount(newName, userId);
	return newAccount;
    }
    
    public Model.Transaction postTransactions(Model.Transaction t, int userId)
	throws DeboException {
	if(t.amount == null || t.currency == null || t.debit == null || t.credit == null) {
	    throw new DeboException(400, "The following fields are required: amount, currency, "
				    + "debit, credit.");
	}
	int newId = model.postTransactions(t, userId);
	Model.Transaction newTx = model.getTransaction(newId, userId);
	return newTx;
    }

    public ArrayList<Model.Currency> getCurrencies(Model.Currency c, int userId)
	throws DeboException {
	return model.getCurrencies(c, userId);
    }
    public ArrayList<Model.Account> getAccounts(Model.Account a, int userId) throws DeboException {
	return model.getAccounts(a, userId);
    }
    public ArrayList<Model.Transaction> getTransactions(Model.TxFilter t, int userId)
	throws DeboException {
	return model.getTransactions(t, userId);
    }

    public Model.Currency getCurrency(String code, int userId) throws DeboException {
	if(code.length() != 3) {
	    throw new DeboException(400, "Currency codes must be three letters long.");
	}
	return model.getCurrency(code, userId);
    }
    public Model.Account getAccount(String name, int userId) throws DeboException {
	if(name.length() == 0) {
	    throw new DeboException(400, "Account names cannot be empty.");
	}
	return model.getAccount(name, userId);
    }
    public Model.Transaction getTransaction(String idString, int userId) throws DeboException {
	int id;
	try {
	    id = Integer.valueOf(idString);
	}
	catch(NumberFormatException e) {
	    throw new DeboException(400, "Transaction IDs must be integers.");
	}
	if(id < 0) {
	    throw new DeboException(400, "Transaction IDs must be positive.");
	}
	return model.getTransaction(id, userId);
    }

    public Model.Currency patchCurrency(String oldCode, Model.Currency c, int userId)
	throws DeboException {
	if(c.code == null && c.name == null && c.type == null) {
	    throw new DeboException(400, "Patchable fields are: code, name, type.");
	}
	if(c.code != null && c.code.length() != 3) {
	    throw new DeboException(400, "Currency codes must be three letters long.");
	}
	String newCode = model.patchCurrency(oldCode, c, userId);
	return model.getCurrency(newCode, userId);
    }
    public Model.Account patchAccount(String oldName, Model.Account a, int userId)
	throws DeboException {
	if(a.type == null && a.name == null) {
	    throw new DeboException(400, "Patchable fields are: type, name.");
	}
	if(a.name != null && a.name.length() == 0) {
	    throw new DeboException(400, "Account names cannot be empty.");
	}
	String newName = model.patchAccount(oldName, a, userId);
	return model.getAccount(newName, userId);
    }
    public Model.Transaction patchTransaction(String refId, Model.Transaction t, int userId)
	throws DeboException {
	int id;
	try {
	    id = Integer.valueOf(refId);
	}
	catch(NumberFormatException e) {
	    throw new DeboException(400, "Transaction IDs must be integers.");
	}
	if(id < 0) {
	    throw new DeboException(400, "Transaction IDs must be positive.");
	}
	if(t.date == null && t.amount == null && t.currency == null && t.debit == null
	   && t.credit == null && t.comment == null) {
	    throw new DeboException(400, "Patchable fields are: date, amount, currency, debit, "
				    + "credit, comment.");
	}
	model.patchTransaction(id, t, userId);
	return model.getTransaction(id, userId);
    }

    public void deleteCurrency(String code, int userId) throws DeboException {
	if(code.length() != 3) {
	    throw new DeboException(400, "Currency codes must be three letters long.");
	}
	model.deleteCurrency(code, userId);
    }
    public void deleteAccount(String name, int userId) throws DeboException {
	if(name.length() == 0) {
	    throw new DeboException(400, "Account names cannot be empty.");
	}
	model.deleteAccount(name, userId);
    }
    public void deleteTransaction(String idString, int userId) throws DeboException {
	int id;
	try {
	    id = Integer.valueOf(idString);
	}
	catch(NumberFormatException e) {
	    throw new DeboException(400, "Transaction IDs must be integers.");
	}
	if(id < 0) {
	    throw new DeboException(400, "Transaction IDs must be positive.");
	}
	model.deleteTransaction(id, userId);
    }

    public HashMap<Model.Account, BigDecimal> getBalance(String accountName, int userId)
	throws DeboException {
	return model.getBalance(accountName, userId);
    }

    public HashMap<Model.Account, BigDecimal> getBalances(int userId)
	throws DeboException {
	return model.getBalances(userId);
    }
}
