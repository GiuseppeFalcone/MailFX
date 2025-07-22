package com.prog3.client.model;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;

public class ClientStorage {
    private String userEmail;
    private final ObservableList<Email> inbox;

    public ClientStorage() {
        this.userEmail = null;
        this.inbox = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    }

    /**
     * Ottiene l'indirizzo email dell'utente.
     *
     * @return L'indirizzo email dell'utente.
     */
    public String getUserEmail() {
        return userEmail;
    }

    /**
     * Imposta l'indirizzo email dell'utente.
     *
     * @param userEmail L'indirizzo email da impostare.
     */
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    /**
     * Ottiene l'ObservableList che rappresenta l'inbox.
     *
     * @return L'inbox come ObservableList.
     */
    public ObservableList<Email> getInbox() {
        return inbox;
    }

    /**
     * Aggiunge un'email all'inbox.
     *
     * @param email L'email da aggiungere.
     */
    public void addEmail(Email email) {
        inbox.add(email);
        FXCollections.sort(inbox, (e1, e2) -> e2.getTimestamp().compareTo(e1.getTimestamp())); // Ordina per data decrescente
    }

    /**
     * Rimuove un'email dall'inbox.
     *
     * @param emailId L'ID dell'email da rimuovere.
     */
    public void removeEmail(String emailId) {
        inbox.removeIf(email -> email.getId().equals(emailId));
    }

    public String mostRecentEmailDate() {
        if (inbox.isEmpty()) {
            return null;
        }
        LocalDateTime mostRecentDate = inbox.getFirst().getTimestamp();
        for (Email email : inbox) {
            if (email.getTimestamp().isAfter(mostRecentDate)) {
                mostRecentDate = email.getTimestamp();
            }
        }
        return mostRecentDate.toString();
    }

}
