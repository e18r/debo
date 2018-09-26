package com.github.emi_silva.debo;

import org.rapidoid.setup.On;
import org.rapidoid.u.U;
import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;

public class Controller {

    private static Logic logic;

    private static int authenticate(Req req) throws Exception {
	String authHeader = req.header("Authorization");
	String sessionToken = authHeader.split(" ")[1];
	int userId = logic.authenticate(sessionToken);
	return userId;
    }

    private static Resp handleException(Exception e, Req req) {
	return req.response().result(U.map("code", 400, "error", e.getMessage(), "status", "Bad Request")).code(400);
    }
    
    public static void main(String[] args)
    {
	logic = new Logic();

	On.get("/login").json((Req req) -> {
		String redirectUri = logic.getOAuthUri();
		return req.response().redirect(redirectUri);
	    });

	On.get("/redirect").json((String code, Req req) -> {
		try {
		    String accessToken = logic.getAccessToken(code);
		    String email = logic.getEmail(accessToken);
		    String sessionToken = logic.getSessionToken(email);
		    return req.response().result(U.map("session_token", sessionToken));
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });

	On.get("/currency_types").json((Req req) -> {
		try {
		    authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		return U.list(logic.getCurrencyTypes());
	    });

 	On.get("/account_types").json((Req req) -> {
		try {
		    authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		return U.list(logic.getAccountTypes());
	    });

	On.post("/currencies").json((Model.Currency c, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		try {
		    Model.Currency newCurrency = logic.postCurrencies(c, userId);
		    return req.response().result(newCurrency).code(201);
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	On.post("/accounts").json((Model.Account a, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		try {
		    Model.Account newAccount = logic.postAccounts(a, userId);
		    return req.response().result(newAccount).code(201);
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	On.post("/transactions").json((Model.Transaction t, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		try {
		    Model.Transaction newTx = logic.postTransactions(t, userId);
		    return req.response().result(newTx).code(201);
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	
	On.get("/currencies").json((Model.Currency c, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		return U.list(logic.getCurrencies(c, userId));
	    });
	On.get("/accounts").json((Model.Account a, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		return U.list(logic.getAccounts(a, userId));
	    });
	On.get("/transactions").json((Model.TxFilter t, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		return U.list(logic.getTransactions(t, userId));
	    });
	
	On.get("/currency/{code}").json((String code, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		return logic.getCurrency(code, userId);
	    });
	On.get("/account/{name}").json((String name, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		return logic.getAccount(name, userId);
	    });
	On.get("/transaction/{id}").json((String id, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		return logic.getTransaction(id, userId);
	    });
	
	On.patch("/currency/{oldCode}").json((String oldCode, Model.Currency c, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		try {
		    return logic.patchCurrency(oldCode, c, userId);
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	On.patch("/account/{oldName}").json((String oldName, Model.Account a, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		try {
		    return logic.patchAccount(oldName, a, userId);
		}
		catch(Exception e) {
		    return handleException(e, req);

		}
	    });
	On.patch("/transaction/{refId}").json((String refId, Model.Transaction t, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		try {
		    return logic.patchTransaction(refId, t, userId);
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	
	On.delete("/currency/{code}").json((String code, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		try {
		    logic.deleteCurrency(code, userId);
		    return "";
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	On.delete("/account/{name}").json((String name, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		try {
		    logic.deleteAccount(name, userId);
		    return "";
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	On.delete("/transaction/{id}").json((String id, Req req) -> {
		int userId;
		try {
		    userId = authenticate(req);
		}
		catch(Exception e) {
		    return req.response().result("").code(401);
		}
		try {
		    logic.deleteTransaction(id, userId);
		    return "";
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
    }
}
