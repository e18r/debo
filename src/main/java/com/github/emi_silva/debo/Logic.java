package com.github.emi_silva.debo;

import java.util.ArrayList;

public class Logic {

    private Model model;

    public Logic() {
	model = new Model();
    }

    public Model.Currency postCurrencies(Model.Currency c) throws Exception {
	String code = model.postCurrencies(c);
	Model.Currency result = model.getCurrency(code);
	return result;
    }
    public Model.Account postAccounts(Model.Account a) throws Exception {
	String name = model.postAccounts(a);
	Model.Account result = model.getAccount(name);
	return result;
    }
    public int postTransactions(Model.Transaction t) throws Exception {
	return model.postTransactions(t);
    }

    public ArrayList<Model.Currency> getCurrencies(Model.Currency c) {
	return model.getCurrencies(c);
    }
    public ArrayList<Model.Account> getAccounts(Model.Account a) {
	return model.getAccounts(a);
    }
    public ArrayList<Model.Transaction> getTransactions(Model.TxFilter t) {
	return model.getTransactions(t);
    }

    public Model.Currency getCurrency(String code) {
	if(code.length() != 3 || !code.matches("[a-zA-Z]*")) {
	    return null;
	}
	return model.getCurrency(code);
    }

    public Model.Account getAccount(String name) throws Exception {
	return model.getAccount(name);
    }

    public Model.Currency patchCurrency(String oldCode, Model.Currency c) throws Exception {
	if(c.code == null && c.name == null && c.type == null) {
	    throw new Exception("Please specify at least one field to patch.");
	}
	String newCode = model.patchCurrency(oldCode, c);
	return model.getCurrency(newCode);
    }
    public Model.Account patchAccount(String oldName, Model.Account a) throws Exception {
	if(a.type == null && a.name == null && a.currency == null) {
	    throw new Exception("Please specify at least one field to patch.");
	}
	String newName = model.patchAccount(oldName, a);
	return model.getAccount(newName);
    }

    public void deleteCurrency(String code) throws Exception {
	model.deleteCurrency(code);
    }
    public void deleteAccount(String name) throws Exception {
	model.deleteAccount(name);
    }
}
