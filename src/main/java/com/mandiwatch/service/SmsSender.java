package com.mandiwatch.service;

/**
 * Anything that can deliver a text message. The project ships with a
 * logging implementation; plugging in MSG91, Twilio or the WhatsApp Cloud
 * API means writing one more class that implements this.
 */
public interface SmsSender {

    /** @return true if the gateway accepted the message */
    boolean send(String phone, String message);

    String channelName();
}
