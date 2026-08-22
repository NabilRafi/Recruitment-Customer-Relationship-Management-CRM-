package com.recruitcrm.notification;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * ADAPTER PATTERN — the "Adaptee".
 *
 * This class speaks SMTP, the protocol mail servers actually use. Its
 * interface is nothing like the one our application wants: instead of a
 * single send() call, it is a back-and-forth conversation of text
 * commands, each of which returns a numeric status code that must be
 * checked before the next command is sent:
 *
 *     EHLO       -> 250
 *     AUTH LOGIN -> 334, then base64 username -> 334, then password -> 235
 *     MAIL FROM  -> 250
 *     RCPT TO    -> 250
 *     DATA       -> 354, then the message, then "." -> 250
 *     QUIT
 *
 * Nothing in the CRM should have to know any of that. SmtpEmailAdapter
 * wraps this class and exposes the simple EmailSender interface instead.
 *
 * Connects over implicit TLS (port 465), which is encrypted from the
 * first byte and avoids the extra STARTTLS negotiation step.
 */
public class SmtpClient {

    private final String host;
    private final int port;
    private final String username;
    private final String password;

    public SmtpClient(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * Runs one full SMTP conversation to deliver a single message.
     * Throws if the server rejects any step.
     */
    public void deliver(String fromAddress, String fromName, String toAddress,
                        String subject, String body) throws IOException {

        SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            socket.setSoTimeout(15000);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            Writer out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);

            expect(in, "220");

            sendLine(out, "EHLO recruitcrm.local");
            expectMultiline(in, "250");

            sendLine(out, "AUTH LOGIN");
            expect(in, "334");

            sendLine(out, Base64.getEncoder().encodeToString(username.getBytes(StandardCharsets.UTF_8)));
            expect(in, "334");

            sendLine(out, Base64.getEncoder().encodeToString(password.getBytes(StandardCharsets.UTF_8)));
            expect(in, "235");   // 235 = authentication accepted

            sendLine(out, "MAIL FROM:<" + fromAddress + ">");
            expect(in, "250");

            sendLine(out, "RCPT TO:<" + toAddress + ">");
            expect(in, "250");

            sendLine(out, "DATA");
            expect(in, "354");   // 354 = go ahead, send the message

            // Message headers, then a blank line, then the body.
            sendLine(out, "From: " + fromName + " <" + fromAddress + ">");
            sendLine(out, "To: " + toAddress);
            sendLine(out, "Subject: " + subject);
            sendLine(out, "MIME-Version: 1.0");
            sendLine(out, "Content-Type: text/plain; charset=UTF-8");
            sendLine(out, "");
            for (String line : body.split("\n")) {
                // A line consisting of a single "." would end the message
                // early, so SMTP requires doubling a leading dot.
                sendLine(out, line.startsWith(".") ? "." + line : line);
            }
            sendLine(out, ".");
            expect(in, "250");

            sendLine(out, "QUIT");
        }
    }

    private void sendLine(Writer out, String line) throws IOException {
        out.write(line + "\r\n");
        out.flush();
    }

    /** Reads one response line and checks it starts with the expected code. */
    private void expect(BufferedReader in, String code) throws IOException {
        String response = in.readLine();
        if (response == null) {
            throw new IOException("Mail server closed the connection unexpectedly");
        }
        if (!response.startsWith(code)) {
            throw new IOException("Mail server said: " + response + " (expected " + code + ")");
        }
    }

    /**
     * EHLO replies span several lines. Continuation lines look like
     * "250-SIZE"; the final line uses a space: "250 SIZE".
     */
    private void expectMultiline(BufferedReader in, String code) throws IOException {
        String response;
        do {
            response = in.readLine();
            if (response == null) {
                throw new IOException("Mail server closed the connection unexpectedly");
            }
            if (!response.startsWith(code)) {
                throw new IOException("Mail server said: " + response + " (expected " + code + ")");
            }
        } while (response.charAt(3) == '-');
    }
}
