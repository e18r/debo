package com.github.emi_silva.debo;

import org.rapidoid.setup.On;

/**
 * Debo
 * 
 */
public class Debo 
{
    public static void main(String[] args)
    {
	On.get("/size").json((String msg) -> msg.length());
    }
}
