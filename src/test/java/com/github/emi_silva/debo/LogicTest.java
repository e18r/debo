package com.github.emi_silva.debo;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class LogicTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public LogicTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( LogicTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testLogic()
    {
        assertTrue( true );
    }
}
