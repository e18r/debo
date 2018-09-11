package com.github.emi_silva.debo;

import java.util.ArrayList;

public class Logic {

    private Model model;

    public Logic() {
	model = new Model();
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

    public void postCurrencies(Model.Currency c) throws Exception {
	model.postCurrencies(c);
    }

    public ArrayList<Model.Account> getAccounts() {
	return model.getAccounts();
    }
}
