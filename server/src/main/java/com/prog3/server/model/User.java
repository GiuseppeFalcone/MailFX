package com.prog3.server.model;

import javafx.collections.ObservableList;

public class User {
    private final String userEmail;
    private final Mailbox mailbox;

    public User(String givenEmail, Mailbox givenMailbox) {
        this.userEmail = givenEmail;
        this.mailbox = givenMailbox != null ? givenMailbox : new Mailbox();
    }

    // Getter for email
    public String getEmail() {
        return userEmail;
    }

    // Getter for mailbox
    public Mailbox getMailbox() {
        return mailbox;
    }

    public ObservableList<Email> getUserMailboxObservableList() {
        return mailbox.getInbox();
    }

    public String getAllUserMailboxForResponse() {
        return mailbox.getMailboxForResponse();
    }

    public String getNewUserMailboxForResponse(String fromDate) {
        return mailbox.getNewMailboxForResponse(fromDate);
    }

    public boolean removeEmail(String emailId) {
        return mailbox.removeEmail(emailId);
    }

    public boolean addEmail(Email email) {
        return mailbox.addEmail(email);
    }
}