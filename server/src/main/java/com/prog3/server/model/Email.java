package com.prog3.server.model;

import java.time.LocalDateTime;
import java.util.*;

public class Email {
    private final String id;
    private final String sender;
    private final List<String> recipients;
    private final String subject;
    private final String body;
    private final LocalDateTime timestamp;

    public Email(String id, String sender, List<String> recipients, String subject, String body, LocalDateTime timestamp) {
        this.id = id;
        this.sender = sender;
        this.recipients = recipients;
        this.subject = subject;
        this.body = body;
        this.timestamp = timestamp;
    }

    public Email(String sender, List<String> recipients, String subject, String body, LocalDateTime timestamp) {
        this.id = UUID.randomUUID().toString();
        this.sender = sender;
        this.recipients = recipients;
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

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getEmailForResponse() {
        return String.join("|",
                id,
                sender,
                String.join("#", recipients), // Concatena i destinatari con #
                subject,
                body,
                timestamp.toString() // Usa il formato predefinito di LocalDateTime
        );
    }
}