package com.github.emi_silva.debo;

import java.util.ArrayList;

public class Logic {

    final int MAX_INT = 2147483647;

    private Model model;

    public Logic() {
	model = new Model();
    }

    public void postCurrencies(Model.Currency c) throws Exception {
	model.postCurrencies(c);
    }
    public void postAccounts(Model.Account a) throws Exception {
	model.postAccounts(a);
    }

    public ArrayList<Model.Currency> getCurrencies(Model.Currency c) {
	return model.getCurrencies(c);
    }
    public ArrayList<Model.Account> getAccounts(Model.Account a) {
	return model.getAccounts(a);
    }

    public Model.Currency getCurrency(String code) {
	if(code.length() != 3 || !code.matches("[a-zA-Z]*")) {
	    return null;
	}
	return model.getCurrency(code);
    }

    public Model.Account getAccount(String idString) throws Exception {
	int id = Integer.valueOf(idString);
	if(id < 0 || id > MAX_INT) {
	    return null;
	}
	return model.getAccount(id);
    }

    public void patchCurrency(String code, Model.Currency c) throws Exception {
	if(c.code == null && c.name == null && c.type == null) {
	    throw new Exception("Please specify at least one field to patch.");
	}
	model.patchCurrency(code, c);
    }
    public void patchAccount(String idString, Model.Account a) throws Exception {
	int id = Integer.valueOf(idString);
	if(id < 0 || id > MAX_INT) {
	    throw new Exception("Invalid account id.");
	}
	if(a.type == null && a.name == null && a.currency == null) {
	    throw new Exception("Please specify at least one field to patch.");
	}
	model.patchAccount(id, a);
    }

    public void deleteCurrency(String code) throws Exception {
	model.deleteCurrency(code);
    }
    public void deleteAccount(String idString) throws Exception {
	int id = Integer.valueOf(idString);
	if(id < 0 || id > MAX_INT) {
	    throw new Exception("Invalid account id.");
	}
	model.deleteAccount(id);
    }
}
