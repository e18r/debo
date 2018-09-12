package com.github.emi_silva.debo;

import java.util.ArrayList;

public class Logic {

    private Model model;

    public Logic() {
	model = new Model();
    }

    public void postCurrencies(Model.Currency c) throws Exception {
	model.postCurrencies(c);
    }

    public ArrayList<Model.Currency> getCurrencies() {
	return model.getCurrencies();
    }

    public Model.Currency getCurrency(String code) {
	if(code.length() != 3 || !code.matches("[a-zA-Z]*")) {
	    return null;
	}
	return model.getCurrency(code);
    }

    public void patchCurrency(String code, Model.Currency c) throws Exception {
	if(c.code == null && c.name == null && c.type == null) {
	    throw new Exception("Please specify at least one field to patch");
	}
	model.patchCurrency(code, c);
    }

    public void deleteCurrency(String code) throws Exception {
	model.deleteCurrency(code);
    }

    public void postAccounts(Model.Account a) throws Exception {
	model.postAccounts(a);
    }

    public ArrayList<Model.Account> getAccounts() {
	return model.getAccounts();
    }
}
