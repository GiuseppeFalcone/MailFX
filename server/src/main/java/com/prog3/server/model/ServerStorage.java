package com.prog3.server.model;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.LocalDateTime;

public class ServerStorage {
    private final String csvFilePath;      // Path to the CSV file
    private ConcurrentHashMap<String, User> users;    // In-memory storage

    /**
     * Constructor that initializes the storage.
     * Loads the users from the csv file if it exists; otherwise, creates an empty storage.
     * @param csvFilePath Path to the csv file.
     */
    public ServerStorage(String csvFilePath) throws Exception {
        this.csvFilePath = csvFilePath;
        users = loadFromFile();
    }

    /**
     * Checks if a user exists in the storage.
     * @param userMail The email address of the user.
     * @return True if the user exists, false otherwise.
     */
    public boolean userExists(String userMail) {
        return users.containsKey(userMail.toLowerCase());
    }

    /**
     * Lists all users in the storage.
     * @return A list of user email addresses.
     */
    public String listUsers() {
        return String.join(", ", users.keySet());
    }

    /**
     * Provides a read-only view of the users map.
     * @return An unmodifiable map containing all users.
     */
    public Map<String, User> getUsers() {
        return Collections.unmodifiableMap(users);
    }

    /**
     * Adds an email to a user's mailbox.
     *
     * @param userMail The email address of the recipient.
     * @param newEmail The email to add.
     */
    public void addEmailToMailbox(String userMail, Email newEmail) throws Exception {
        boolean added = users.get(userMail.toLowerCase()).addEmail(newEmail);
        if (added) {
            appendToCSV(userMail, newEmail);
        }
    }

    public boolean removeEmailFromMailbox(String userEmail, String emailId) throws Exception {
        boolean removed = users.get(userEmail.toLowerCase()).removeEmail(emailId);
        if (removed) {
            removeFromCSV(userEmail, emailId); // Rimuovi solo la mail specifica dal file
        }
        return removed;
    }

    public String getAllUserEmails(String clientMail) {
        return users.get(clientMail.toLowerCase()).getAllUserMailboxForResponse();
    }

    public String getNewUserEmails(String clientMail, String fromDate) {
        return users.get(clientMail.toLowerCase()).getNewUserMailboxForResponse(fromDate);
    }

    /**
     * Loads user data from the CSV file into the ConcurrentHashMap.
     * @return A ConcurrentHashMap with the user data.
     */
    private synchronized ConcurrentHashMap<String, User> loadFromFile() throws Exception {
        ConcurrentHashMap<String, User> userMap = new ConcurrentHashMap<>();
        File file = new File(csvFilePath);

        if (!file.exists()) {
            throw new FileNotFoundException("File not found with path: " + csvFilePath);
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length != 7) {
                    throw new Exception("Invalid CSV file format");
                }

                if (parts[1].equals(" ")) {
                    String userMail = parts[0];
                    userMap.putIfAbsent(userMail, new User(userMail, new Mailbox()));
                    continue;
                }

                String userEmail = parts[0].toLowerCase();
                String emailId = parts[1];
                String sender = parts[2];
                List<String> receivers = Arrays.asList(parts[3].split("#"));
                String subject = parts[4];
                String body = parts[5];
                LocalDateTime timestamp = LocalDateTime.parse(parts[6]);

                Email email = new Email(emailId, sender, receivers, subject, body, timestamp);

                // Add the email to the user's mailbox
                userMap.computeIfAbsent(userEmail, k -> new User(userEmail, new Mailbox()))
                        .getMailbox().addEmail(email);
            }
        } catch (Exception e) {
            throw new FileNotFoundException("Error in reading csv file with path: " + csvFilePath);
        }
        return userMap;
    }

    // Metodo per aggiungere una nuova email al file CSV
    private synchronized void appendToCSV(String userEmail, Email email) throws Exception {
        try (PrintWriter writer = new PrintWriter(new FileWriter(csvFilePath, true))) {
            writer.printf(
                    "%s|%s|%s|%s|%s|%s|%s%n",
                    userEmail.toLowerCase(),
                    email.getId(),
                    email.getSender(),
                    String.join("#", email.getRecipients()),
                    email.getSubject(),
                    email.getBody(),
                    email.getTimestamp().toString()
            );
        } catch (IOException e) {
            throw new Exception("Error writing new data to file with path: " + csvFilePath);
        }
    }

    private synchronized void removeFromCSV(String userEmail, String emailId) throws Exception {
        File inputFile = new File(csvFilePath);
        File tempFile = new File(csvFilePath + ".tmp");

        try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
             PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 2 && parts[0].equals(userEmail.toLowerCase()) && parts[1].equals(emailId)) {
                    // Salta questa riga perché corrisponde all'email da rimuovere
                    continue;
                }
                writer.println(line);
            }

            reader.close();
            writer.close();

            // Elimina il file originale e rinomina il temporaneo
            if (!inputFile.delete()) {
                throw new Exception("Error deleting original file: " + csvFilePath);
            }

            // Sovrascrivi il file originale con il file temporaneo
            if (!tempFile.renameTo(inputFile)) {
                throw new Exception("Error in renaming tmp file to csv file");
            }
        } catch (IOException e) {
            throw new Exception("IOError while removing mail from file with path: " + csvFilePath);
        }
    }

    public synchronized void reloadFromFile() throws Exception {
        try {
            ConcurrentHashMap<String, User> loadedUsers = loadFromFile();
            users = loadedUsers;
        } catch (Exception e) {
            throw new Exception("Error in reloading CSV File");
        }
    }
}