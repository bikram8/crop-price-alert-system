package com.mandiwatch.service;

/** A farmer with this mobile number already exists. Replaces a generic IllegalArgumentException. */
public class DuplicatePhoneException extends RuntimeException {

    public DuplicatePhoneException(String phone) {
        super("A farmer with phone " + phone + " is already registered.");
    }
}
