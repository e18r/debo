package com.github.emi_silva.debo;

import java.util.HashMap;

class DeboException extends Exception {

    public static final HashMap<Integer, String> STATUSES;
    public static final HashMap<Integer, String> ERRORS;
    static {
	STATUSES = new HashMap<Integer, String>();
	ERRORS = new HashMap<Integer, String>();
	STATUSES.put(400, "Bad Request");
	ERRORS.put(400, "The server cannot or will not process the request.");
	STATUSES.put(401, "Unauthorized");
	ERRORS.put(401, "Authentication is required and has failed or has not yet been provided.");
	STATUSES.put(404, "Not Found");
	ERRORS.put(404, "The requested resource could not be found.");
	STATUSES.put(500, "Internal Server Error");
	ERRORS.put(500, "An unexpected condition was encountered.");
	STATUSES.put(412, "Precondition Failed");
	ERRORS.put(412, "Please create a new token and/or user.");
    }
    
    public int code;
    public String error;

    public DeboException(int code, String error) {
	this.code = code;
	this.error = error;
    }

    public DeboException(int code) {
	this.code = code;
	this.error = ERRORS.get(code);
    }

    public HashMap toMap() {
	HashMap<String, Object> map = new HashMap<String, Object>();
	map.put("code", code);
	map.put("status", STATUSES.get(code));
	map.put("error", error);
	return map;
    }

}
