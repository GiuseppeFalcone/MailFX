# MailFX

This project is a Java-based email client-server application. It consists of two separate modules: `client` and `server`. Both are JavaFX desktop applications, with the client providing the user interface and the server handling email storage and communication.

## Features

### Client

- **Login**: Users can log in using their email address.
- **Inbox**: View received emails, including sender, recipients, subject, and body.
- **Compose Email**: Write and send new emails to one or multiple recipients.
- **Reply/Reply All**: Respond to emails directly or to all recipients.
- **Forward**: Forward emails to other recipients.
- **Delete**: Remove emails from the inbox.
- **Real-time Updates**: Automatically fetch new emails periodically.

### Server

- **Email Storage**: Emails are stored in a CSV file.
- **Socket Communication**: Handles client requests for login, email retrieval, sending, and deletion.

## Project Structure

```
.
├── client/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── com.prog3.client/
│   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── InboxController.java
│   │   │   │   │   │   ├── LoginController.java
│   │   │   │   │   │   ├── WriteMailController.java
│   │   │   │   │   ├── library/
│   │   │   │   │   │   ├── AlertNotification.java
│   │   │   │   │   │   ├── Check.java
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── ClientStorage.java
│   │   │   │   │   │   ├── Email.java
│   │   │   │   │   ├── ClientMain.java
│   │   │   │   ├── module-info.java
│   │   ├── resources/
│   │   │   ├── com.prog3.client/
│   │   │   │   ├── login-view.fxml
│   │   │   │   ├── inbox-view.fxml
│   │   │   │   ├── writeMail-view.fxml
│   ├── pom.xml
├── server/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   ├── com.prog3.server/
│   │   │   │   │   ├── controller/
│   │   │   │   │   │   ├── ServerController.java
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Email.java
│   │   │   │   │   │   ├── Mailbox.java
│   │   │   │   │   │   ├── ServerStorage.java
│   │   │   │   │   │   ├── User.java
│   │   │   │   │   │   ├── data/
│   │   │   │   │   │   │   ├── storage.csv
│   │   │   │   │   ├── ServerMain.java
│   │   ├── module-info.java
│   ├── pom.xml
```

## Requirements

- Java 23 or higher
- Maven 3.8.5 or higher

## Setup

### Server

1. Navigate to the `server` directory.
2. Run the following command to start the server:
   ```sh
   ./mvnw javafx:run
   ```

### Client

1. Navigate to the `client` directory.
2. Run the following command to start the client:
   ```sh
   ./mvnw javafx:run
   ```

## Usage

1. Start the server.
2. Launch the client application.
3. Log in using a valid email address (e.g., `user1@mail.com`).
4. Use the client interface to view, send, reply, forward, or delete emails.

## Authors

- **Giuseppe Falcone**
  - [LinkedIn](https://www.linkedin.com/in/giuseppefalcone01/)
  - Email: giuseppe001falcone@gmail.com
- **Stella Cacciatore**

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
