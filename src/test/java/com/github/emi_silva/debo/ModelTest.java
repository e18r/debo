package com.github.emi_silva.debo;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import java.util.Properties;
import java.util.ArrayList;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;

public class ModelTest extends TestCase {

    private Model model;
    
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ModelTest( String testName ) {
        super(testName);	
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(ModelTest.class);
    }

    /**
     * Checks the database is initialized correctly
     */
    public void testModel() throws Exception {
	model = new Model();
        assertNotNull(model.conn);
    }

    /**
     * Checks properties are read
     */
    public void testReadProps() throws Exception {
	model = new Model();
	Properties props = model.readProps("/db.properties");
	String schema = props.getProperty("currentSchema");
	assertEquals(schema, "debo");
    }

    /**
     * Checks currency types are returned correctly
     */
    public void testGetCurrencyTypes() throws Exception {
	model = new Model();
	ArrayList<Model.CurrencyType> cts = model.getCurrencyTypes();
	for(Model.CurrencyType ct : cts) {
	    if(ct.name.equals("fiat")) {
		return;
	    }
	}
	throw new Exception("fiat currency type not found");
    }

    /**
     * Checks an id is found for an existing currency type name
     */
    public void testFindCurrencyTypeId() throws Exception {
	model = new Model();
	assertEquals(model.findCurrencyTypeId("fiat"), 1);
    }

    /**
     * Checks account types are returned correctly
     */
    public void testGetAccountTypes() throws Exception {
	model = new Model();
	ArrayList<Model.AccountType> ats = model.getAccountTypes();
	for(Model.AccountType at : ats) {
	    if(at.name.equals("asset")) {
		return;
	    }
	}
	throw new Exception("asset account type not found");
    }

    /**
     * Checks an account type id is found
     */
    public void testFindAccountTypeId() throws Exception {
	model = new Model();
	int id = model.findAccountTypeId("asset");
	assertEquals(id, 1);
    }

}
