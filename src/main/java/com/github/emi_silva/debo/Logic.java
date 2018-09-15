package com.github.emi_silva.debo;

import java.util.Properties;
import java.security.SecureRandom;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Logic {

    final int MAX_INT = 2147483647;
    private Model model;
    private Properties authProps;
    private SecureRandom random;
    private static final char[] symbols = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();

    public Logic() {
	model = new Model();
	authProps = model.readProps("/auth.properties");
	byte[] seed = authProps.getProperty("seed").getBytes();
	random = new SecureRandom(seed);
    }

    /**
     * Builds the Google OAuth URI the client is redirected to for authorization
     * TODO: state (for added security or to store what the user was doing)
     */
    public String getOAuthUri() {
	String OAuthEndpoint = authProps.getProperty("oauthEndpoint");
	String clientId = authProps.getProperty("clientId");
	String redirectUri = authProps.getProperty("redirectUri");
	String scope = authProps.getProperty("scope");
	String responseType = authProps.getProperty("responseType");
	String state = "state"; // TODO
	String uri = OAuthEndpoint + "?client_id=" + clientId + "&redirect_uri=" + redirectUri
	    + "&scope=" + scope + "&response_type=" + responseType + "&state=" + state;
	return uri;
    }

    /**
     * Given a code provider by the authenticated user, gets an access token
     */
    public String getAccessToken(String code) throws UnirestException {
	String accessTokenEndpoint = authProps.getProperty("accessTokenEndpoint");
	String contentHeader = authProps.getProperty("contentHeader");
	String clientId = authProps.getProperty("clientId");
	String clientSecret = authProps.getProperty("clientSecret");
	String redirectUri = authProps.getProperty("redirectUri");
	String grantType = authProps.getProperty("grantType");
	HttpResponse<JsonNode> response = Unirest.post(accessTokenEndpoint)
	    .header("Content-Type", contentHeader)
	    .field("code", code)
	    .field("client_id", clientId)
	    .field("client_secret", clientSecret)
	    .field("redirect_uri", redirectUri)
	    .field("grantType", grantType)
	    .asJson();
	JSONObject body = new JSONObject(response.getBody().toString());
	String accessToken = body.getString("access_token");
	return accessToken;
    }

    /**
     * Gets the user's email given the access token
     */
    public String getEmail(String accessToken) throws UnirestException {
	String APIEndpoint = authProps.getProperty("APIEndpoint");
	String alt = authProps.getProperty("alt");
	String Uri = APIEndpoint + "?alt=" + alt;
	String authHeader = authProps.getProperty("authHeader") + " " + accessToken;
	HttpResponse<JsonNode> response = Unirest.get(Uri)
	    .header("Authorization", authHeader)
	    .asJson();
	JSONObject body = new JSONObject(response.getBody().toString());
	String email = body.getString("email");
	return email;
    }

    /**
     * Generates a random string
     * Inspired from https://stackoverflow.com/a/41156/2430274
     */
    public String newRandomString(int length) {
	char[] token = new char[length];
	for(int i = 0; i < token.length; i++) {
	    token[i] = symbols[random.nextInt(symbols.length)];
	}
	return new String(token);
    }

    /**
     * Retreives or creates a session token, and returns it to the user
     */
    public String getSessionToken(String email) throws Exception {
	String sessionToken;
	try {
	    sessionToken = model.getSessionToken(email);
	}
	catch(Exception e) {
	    sessionToken = newRandomString(64);
	    model.newUser(email, sessionToken);
	}
	return sessionToken;
    }

    public int authenticate(String sessionToken) throws Exception {
	return model.authenticate(sessionToken);
    }

    public Model.Currency postCurrencies(Model.Currency c, int userId) throws Exception {
	if(c.code == null) {
	    throw new Exception("Please specify a currency code");
	}
	if(c.name == null) {
	    throw new Exception("Please specify a currency name.");
	}
	if(c.type == null) {
	    throw new Exception("Please specify a currency type.");
	}
	String newCode = model.postCurrencies(c, userId);
	Model.Currency newCurrency = model.getCurrency(newCode, userId);
	return newCurrency;
    }
    public Model.Account postAccounts(Model.Account a, int userId) throws Exception {
	if(a.type == null) {
	    throw new Exception("Please specify an account type.");
	}
	if(a.name == null) {
	    throw new Exception("Please specify an account name.");
	}
	if(a.currency == null) {
	    throw new Exception("Please specify a currency code.");
	}
	String newName = model.postAccounts(a, userId);
	Model.Account newAccount = model.getAccount(newName, userId);
	return newAccount;
    }

    private void checkCurrencies(String debit, String credit, int userId) throws Exception {
	Model.Account debitAccount = model.getAccount(debit, userId);
	Model.Account creditAccount = model.getAccount(credit, userId);
	if(!debitAccount.currency.equals(creditAccount.currency)) {
	    throw new Exception("Currency mismatch.");
	}
    }
    
    public Model.Transaction postTransactions(Model.Transaction t, int userId) throws Exception {
	if(t.amount == null) {
	    throw new Exception("Please specify an amount.");
	}
	if(t.debit == null) {
	    throw new Exception("Please specify the name of the account to debit from.");
	}
	if(t.credit == null) {
	    throw new Exception("Please specify the name of the account to credit from.");
	}
	checkCurrencies(t.debit, t.credit, userId);
	int newId = model.postTransactions(t, userId);
	Model.Transaction newTx = model.getTransaction(newId, userId);
	return newTx;
    }

    public ArrayList<Model.Currency> getCurrencies(Model.Currency c, int userId) {
	return model.getCurrencies(c, userId);
    }
    public ArrayList<Model.Account> getAccounts(Model.Account a, int userId) {
	return model.getAccounts(a, userId);
    }
    public ArrayList<Model.Transaction> getTransactions(Model.TxFilter t, int userId) {
	return model.getTransactions(t, userId);
    }

    public Model.Currency getCurrency(String code, int userId) {
	if(code.length() != 3 || !code.matches("[a-zA-Z]*")) {
	    return null;
	}
	return model.getCurrency(code, userId);
    }
    public Model.Account getAccount(String name, int userId) {
	return model.getAccount(name, userId);
    }
    public Model.Transaction getTransaction(String idString, int userId) {
	int id;
	try {
	    id = Integer.valueOf(idString);
	}
	catch(Exception e) {
	    return null;
	}
	if(id < 0 || id > MAX_INT) {
	    return null;
	}
	return model.getTransaction(id, userId);
    }

    public Model.Currency patchCurrency(String oldCode, Model.Currency c, int userId) throws Exception {
	if(c.code == null && c.name == null && c.type == null) {
	    throw new Exception("Please specify at least one field to patch.");
	}
	String newCode = model.patchCurrency(oldCode, c, userId);
	return model.getCurrency(newCode, userId);
    }
    public Model.Account patchAccount(String oldName, Model.Account a, int userId) throws Exception {
	if(a.type == null && a.name == null && a.currency == null) {
	    throw new Exception("Please specify at least one field to patch.");
	}
	String newName = model.patchAccount(oldName, a, userId);
	return model.getAccount(newName, userId);
    }
    public Model.Transaction patchTransaction(String idString, Model.Transaction t, int userId) throws Exception {
	int id;
	id = Integer.valueOf(idString);
	if(t.date == null && t.amount == null && t.debit == null && t.credit == null
	   && t.comment == null) {
	    throw new Exception("Please specify at least one field to patch.");
	}
	Model.Transaction oldTx = model.getTransaction(id, userId);
	String debit;
	if(t.debit != null) {
	    debit = t.debit;
	}
	else {
	    debit = oldTx.debit;
	}
	String credit;
	if(t.credit != null) {
	    credit = t.credit;
	}
	else {
	    credit = oldTx.credit;
	}
	checkCurrencies(debit, credit, userId);
	model.patchTransaction(id, t, userId);
	return model.getTransaction(id, userId);
    }

    public void deleteCurrency(String code, int userId) throws Exception {
	model.deleteCurrency(code, userId);
    }
    public void deleteAccount(String name, int userId) throws Exception {
	model.deleteAccount(name, userId);
    }
    public void deleteTransaction(String idString, int userId) throws Exception {
	int id;
	id = Integer.valueOf(idString);
	model.deleteTransaction(id, userId);
    }
}
