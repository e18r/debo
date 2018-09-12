package com.github.emi_silva.debo;

import org.rapidoid.setup.On;
import org.rapidoid.u.U;
import org.rapidoid.http.Req;

public class Controller {
    
    public static void main(String[] args)
    {
	Logic logic = new Logic();

	On.post("/currencies").json((Model.Currency c, Req req) -> {
		try {
		    logic.postCurrencies(c);
		    return req.response().result("").code(201);
		}
		catch(Exception e) {
		    return req.response().result(U.map("code", 400, "error", e.getMessage(), "status", "Bad Request")).code(400);
		}
	    });
	On.post("/accounts").json((Model.Account a, Req req) -> {
		try {
		    logic.postAccounts(a);
		    return req.response().result("").code(201);
		}
		catch(Exception e) {
		    return req.response().result(U.map("code", 400, "error", e.getMessage(), "status", "Bad Request")).code(400);
		}
	    });
	On.post("/transactions").json((Model.Transaction t, Req req) -> {
		try {
		    logic.postTransactions(t);
		    return req.response().result("").code(201);
		}
		catch(Exception e) {
		    return req.response().result(U.map("code", 400, "error", e.getMessage(), "status", "Bad Request")).code(400);
		}
	    });
	
	On.get("/currencies").json((Model.Currency c) -> U.list(logic.getCurrencies(c)));
	On.get("/accounts").json((Model.Account a) -> U.list(logic.getAccounts(a)));
	
	On.get("/currency/{code}").json((String code) -> logic.getCurrency(code));
	On.get("/account/{id}").json((String id, Req req) -> {
		try {
		    return logic.getAccount(id);
		}
		catch(Exception e) {
		return req.response().result(U.map("code", 400, "error", e.getMessage(), "status", "Bad Request")).code(400);
		}
	    });
	
	On.patch("/currency/{cod}").json((String cod, Model.Currency c, Req req) -> {
		try {
		    logic.patchCurrency(cod, c);
		    return "";
		}
		catch(Exception e) {
		    return req.response().result(U.map("code", 400, "error", e.getMessage(), "status", "Bad Request")).code(400);
		}
	    });
	On.patch("/account/{idn}").json((String idn, Model.Account a, Req req) -> {
		try {
		    logic.patchAccount(idn, a);
		    return "";
		}
		catch(Exception e) {
		    return req.response().result(U.map("code", 400, "error", e.getMessage(), "status", "Bad Request")).code(400);
		}
	    });
	
	On.delete("/currency/{code}").json((String code, Req req) -> {
		try {
		    logic.deleteCurrency(code);
		    return "";
		}
		catch(Exception e) {
		    return req.response().result(U.map("code", 400, "error", e.getMessage(), "status", "Bad Request")).code(400);
		}
	    });
	On.delete("/account/{id}").json((String id, Req req) -> {
		try {
		    logic.deleteAccount(id);
		    return "";
		}
		catch(Exception e) {
		    return req.response().result(U.map("code", 400, "error", e.getMessage(), "status", "Bad Request")).code(400);
		}
	    });
    }
}
