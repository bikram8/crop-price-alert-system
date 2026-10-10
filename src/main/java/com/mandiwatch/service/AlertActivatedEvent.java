package com.mandiwatch.service;

/** Published when an alert is created or resumed, so it can be checked against today's prices once saved. */
public record AlertActivatedEvent(Long alertId) {
}
