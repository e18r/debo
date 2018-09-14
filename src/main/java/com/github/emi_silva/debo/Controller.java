package com.github.emi_silva.debo;

import org.rapidoid.setup.On;
import org.rapidoid.u.U;
import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;

public class Controller {

    private static Resp handleException(Exception e, Req req) {
	return req.response().result(U.map("code", 400, "error", e.getMessage(), "status", "Bad Request")).code(400);
    }
    
    public static void main(String[] args)
    {
	Logic logic = new Logic();

	On.post("/currencies").json((Model.Currency c, Req req) -> {
		try {
		    Model.Currency newCurrency = logic.postCurrencies(c);
		    return req.response().result(newCurrency).code(201);
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	On.post("/accounts").json((Model.Account a, Req req) -> {
		try {
		    Model.Account newAccount = logic.postAccounts(a);
		    return req.response().result(newAccount).code(201);
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	On.post("/transactions").json((Model.Transaction t, Req req) -> {
		try {
		    Model.Transaction newTx = logic.postTransactions(t);
		    return req.response().result(newTx).code(201);
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	
	On.get("/currencies").json((Model.Currency c) -> U.list(logic.getCurrencies(c)));
	On.get("/accounts").json((Model.Account a) -> U.list(logic.getAccounts(a)));
	On.get("/transactions").json((Model.TxFilter t) -> U.list(logic.getTransactions(t)));
	
	On.get("/currency/{code}").json((String code) -> logic.getCurrency(code));
	On.get("/account/{name}").json((String name) -> logic.getAccount(name));
	On.get("/transaction/{idString}").json((String idString) -> logic.getTransaction(idString));
	
	On.patch("/currency/{oldCode}").json((String oldCode, Model.Currency c, Req req) -> {
		try {
		    return logic.patchCurrency(oldCode, c);
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	On.patch("/account/{oldName}").json((String oldName, Model.Account a, Req req) -> {
		try {
		    return logic.patchAccount(oldName, a);
		}
		catch(Exception e) {
		    return handleException(e, req);

		}
	    });
	On.patch("/transaction/{id}").json((String id, Model.Transaction t, Req req) -> {
		try {
		    return logic.patchTransaction(id, t);
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	
	On.delete("/currency/{code}").json((String code, Req req) -> {
		try {
		    logic.deleteCurrency(code);
		    return "";
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	On.delete("/account/{name}").json((String name, Req req) -> {
		try {
		    logic.deleteAccount(name);
		    return "";
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
	On.delete("/transaction/{id}").json((String id, Req req) -> {
		try {
		    logic.deleteTransaction(id);
		    return "";
		}
		catch(Exception e) {
		    return handleException(e, req);
		}
	    });
    }
}
