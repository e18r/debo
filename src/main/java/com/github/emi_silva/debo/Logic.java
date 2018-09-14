package com.github.emi_silva.debo;

import java.util.ArrayList;

public class Logic {

    final int MAX_INT = 2147483647;

    private Model model;

    public Logic() {
	model = new Model();
    }

    public Model.Currency postCurrencies(Model.Currency c) throws Exception {
	if(c.code == null) {
	    throw new Exception("Please specify a currency code");
	}
	if(c.name == null) {
	    throw new Exception("Please specify a currency name.");
	}
	if(c.type == null) {
	    throw new Exception("Please specify a currency type.");
	}
	String newCode = model.postCurrencies(c);
	Model.Currency newCurrency = model.getCurrency(newCode);
	return newCurrency;
    }
    public Model.Account postAccounts(Model.Account a) throws Exception {
	if(a.type == null) {
	    throw new Exception("Please specify an account type.");
	}
	if(a.name == null) {
	    throw new Exception("Please specify an account name.");
	}
	if(a.currency == null) {
	    throw new Exception("Please specify a currency code.");
	}
	String newName = model.postAccounts(a);
	Model.Account newAccount = model.getAccount(newName);
	return newAccount;
    }

    private void checkCurrencies(String debit, String credit) throws Exception {
	Model.Account debitAccount = model.getAccount(debit);
	Model.Account creditAccount = model.getAccount(credit);
	if(!debitAccount.currency.equals(creditAccount.currency)) {
	    throw new Exception("Currency mismatch.");
	}
    }
    
    public Model.Transaction postTransactions(Model.Transaction t) throws Exception {
	if(t.amount == null) {
	    throw new Exception("Please specify an amount.");
	}
	if(t.debit == null) {
	    throw new Exception("Please specify the name of the account to debit from.");
	}
	if(t.credit == null) {
	    throw new Exception("Please specify the name of the account to credit from.");
	}
	checkCurrencies(t.debit, t.credit);
	int newId = model.postTransactions(t);
	Model.Transaction newTx = model.getTransaction(newId);
	return newTx;
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
    public Model.Account getAccount(String name) {
	return model.getAccount(name);
    }
    public Model.Transaction getTransaction(String idString) {
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
	return model.getTransaction(id);
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
    public Model.Transaction patchTransaction(String idString, Model.Transaction t) throws Exception {
	int id;
	id = Integer.valueOf(idString);
	if(t.date == null && t.amount == null && t.debit == null && t.credit == null
	   && t.comment == null) {
	    throw new Exception("Please specify at least one field to patch.");
	}
	Model.Transaction oldTx = model.getTransaction(id);
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
	checkCurrencies(debit, credit);
	model.patchTransaction(id, t);
	return model.getTransaction(id);
    }

    public void deleteCurrency(String code) throws Exception {
	model.deleteCurrency(code);
    }
    public void deleteAccount(String name) throws Exception {
	model.deleteAccount(name);
    }
    public void deleteTransaction(String idString) throws Exception {
	int id;
	id = Integer.valueOf(idString);
	model.deleteTransaction(id);
    }
}
