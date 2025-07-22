package com.prog3.server.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;

public class Mailbox {
    private final ObservableList<Email> inbox;

    public Mailbox() {
        this.inbox = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    }

    public ObservableList<Email> getInbox() {
        return FXCollections.unmodifiableObservableList(inbox);
    }

    public boolean addEmail(Email email) {
        if (!inbox.contains(email)) {
            inbox.add(email);
            return true;
        } return false;
    }

    public boolean removeEmail(String emailId) {
        return inbox.removeIf(email -> email.getId().equals(emailId));
    }

    public String getMailboxForResponse () {
        if (inbox.isEmpty()) {
            return "";
        }

        StringBuilder response = new StringBuilder();
        for (Email email : inbox) {
            response.append(email.getEmailForResponse());
            response.append("±");
        }
        return response.toString();
    }

    public String getNewMailboxForResponse (String fromDate) {
        if (inbox.isEmpty()) {
            return "";
        }

        StringBuilder response = new StringBuilder();
        LocalDateTime givenDate = LocalDateTime.parse(fromDate);

        for (Email email : inbox) {
            if (email.getTimestamp().isAfter(givenDate)) {
                response.append(email.getEmailForResponse());
                response.append("±");
            }
        }
        return response.toString();
    }
}