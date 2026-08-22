package com.recruitcrm.web;


public class AuthFailure extends RuntimeException {
    public AuthFailure(String message) {
        super(message);
    }
}
