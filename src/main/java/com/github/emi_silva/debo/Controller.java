package com.github.emi_silva.debo;

import org.rapidoid.setup.App;
import org.rapidoid.setup.On;
import org.rapidoid.u.U;
import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;
import java.util.ArrayList;
import java.time.Instant;
import java.util.HashMap;
import org.rapidoid.setup.My;

public class Controller {

    private static Logic logic;

    private static int authenticate(Req req) throws DeboException {
	String authHeader;
	try {
	    authHeader = req.header("Authorization");
	}
	catch(IllegalArgumentException e) {
	    throw new DeboException(401, "No Authorization header present.");
	}
	int userId = logic.authenticate(authHeader);
	return userId;
    }

    private static Resp showError(DeboException e, Req req) {
	return req.response().json(e.toMap()).code(e.code);
    }
    
    public static void main(String[] args) {
	try {
	    logic = new Logic();
	}
	catch(Exception e) {
	    System.err.println(e.toString());
	    return;
	}
	App.bootstrap(args);

	My.errorHandler((req, resp, error) -> {
		return showError(new DeboException(400), req);
	    });

	On.req(req -> {
		try {
		    authenticate(req);
		    throw new DeboException(404, "The requested URL and method do not exist.");
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });

	On.get("/login").json((Req req) -> {
		String redirectUri = logic.getOAuthUri();
		return req.response().redirect(redirectUri);
	    });

	On.get("/redirect").json((String code, Req req) -> {
		try {
		    String accessToken = logic.getAccessToken(code);
		    String email = logic.getEmail(accessToken);
		    HashMap<String, String> session = logic.getSession(email);
		    return req.response().json(session);
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });

	On.get("/logout").json((Req req) -> {
		try {
		    int userId = authenticate(req);
		    logic.logout(userId);
		    return req.response().json("");
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });

	On.get("/currency_types").json((Req req) -> {
		try {
		    authenticate(req);
		    return U.list(logic.getCurrencyTypes());
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });

 	On.get("/account_types").json((Req req) -> {
		try {
		    authenticate(req);
		    return U.list(logic.getAccountTypes());
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });

	On.post("/currencies").json((Model.Currency c, Req req) -> {
		try {
		    int userId = authenticate(req);
		    Model.Currency newCurrency = logic.postCurrencies(c, userId);
		    return req.response().result(newCurrency).code(201);
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	On.post("/accounts").json((Model.Account a, Req req) -> {
		try {
		    int userId = authenticate(req);
		    Model.Account newAccount = logic.postAccounts(a, userId);
		    return req.response().result(newAccount).code(201);
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	On.post("/transactions").json((Model.Transaction t, Req req) -> {
		try {
		    int userId = authenticate(req);
		    Model.Transaction newTx = logic.postTransactions(t, userId);
		    return req.response().result(newTx).code(201);
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	
	On.get("/currencies").json((Model.Currency c, Req req) -> {
		try {
		    int userId = authenticate(req);
		    return U.list(logic.getCurrencies(c, userId));
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	On.get("/accounts").json((Model.Account a, Req req) -> {
		try {
		    int userId = authenticate(req);
		    return U.list(logic.getAccounts(a, userId));
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	On.get("/transactions").json((Model.TxFilter t, Req req) -> {
		try {
		    int userId = authenticate(req);
		    return U.list(logic.getTransactions(t, userId));
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	
	On.get("/currency/{code}").json((String code, Req req) -> {
		try {
		    int userId = authenticate(req);
		    return logic.getCurrency(code, userId);
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	On.get("/account/{name}").json((String name, Req req) -> {
		try {
		    int userId = authenticate(req);
		    return logic.getAccount(name, userId);
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	On.get("/transaction/{id}").json((String id, Req req) -> {
		try {
		    int userId = authenticate(req);
		    return logic.getTransaction(id, userId);
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	
	On.patch("/currency/{oldCode}").json((String oldCode, Model.Currency c, Req req) -> {
		try {
		    int userId = authenticate(req);
		    return logic.patchCurrency(oldCode, c, userId);
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	On.patch("/account/{oldName}").json((String oldName, Model.Account a, Req req) -> {
		try {
		    int userId = authenticate(req);
		    return logic.patchAccount(oldName, a, userId);
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	On.patch("/transaction/{refId}").json((String refId, Model.Transaction t, Req req) -> {
		try {
		    int userId = authenticate(req);
		    return logic.patchTransaction(refId, t, userId);
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	
	On.delete("/currency/{code}").json((String code, Req req) -> {
		try {
		    int userId = authenticate(req);
		    logic.deleteCurrency(code, userId);
		    return req.response().json("");
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	On.delete("/account/{name}").json((String name, Req req) -> {
		try {
		    int userId = authenticate(req);
		    logic.deleteAccount(name, userId);
		    return req.response().json("");
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
	On.delete("/transaction/{id}").json((String id, Req req) -> {
		try {
		    int userId = authenticate(req);
		    logic.deleteTransaction(id, userId);
		    return req.response().json("");
		}
		catch(DeboException e) {
		    return showError(e, req);
		}
	    });
    }
}
