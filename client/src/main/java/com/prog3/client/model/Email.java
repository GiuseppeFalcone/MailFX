package com.prog3.client.model;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public class Email {
    private final String id;
    private final String sender;
    private final List<String> recipients;
    private final String subject;
    private final String body;
    private final LocalDateTime timestamp;

    /**
     * The constructor of the email class. It is used one an object from
     *
     * @param id
     * @param sender
     * @param recipients
     * @param subject
     * @param body
     * @param timestamp
     */
    public Email(String id, String sender, List<String> recipients, String subject, String body, LocalDateTime timestamp) {
        this.id = id;
        this.sender = sender;
        this.recipients = Collections.unmodifiableList(recipients);
        this.subject = subject;
        this.body = body;
        this.timestamp = timestamp;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getSender() {
        return sender;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    public String getRecipientsAsString() {
        return String.join(", ", recipients);
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}