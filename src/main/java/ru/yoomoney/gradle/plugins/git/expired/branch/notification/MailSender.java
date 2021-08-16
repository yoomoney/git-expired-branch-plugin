package ru.yoomoney.gradle.plugins.git.expired.branch.notification;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import ru.yoomoney.gradle.plugins.git.expired.branch.settings.EmailConnectionSettings;

import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Класс для отправки email
 *
 * @author Vasily Sozykin
 *         Date: 09.03.2017.
 */
public class MailSender {
    private final Logger log = Logging.getLogger(MailSender.class);
    private final EmailConnectionSettings settings;

    public MailSender(EmailConnectionSettings settings) {
        this.settings = settings;
    }

    /**
     * Отправить email получателям от имени служебного пользователя Jira для релизов
     *
     * @param sender     email отправителя письма
     * @param recipients список получателей
     * @param subject    тема письма
     * @param body       текст письма
     */
    public void sendEmail(String sender, Set<String> recipients, String subject, String body) {
        Properties props = new Properties();
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.starttls.enable", "false");
        props.setProperty("mail.smtp.host", settings.getEmailHost());
        props.setProperty("mail.smtp.port", String.valueOf(settings.getEmailPort()));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(settings.getEmailAuthUser(), settings.getEmailAuthPassword());
            }
        });

        Set<Address> recipientSet = recipients.stream()
                .map(recipient -> {
                    try {
                        return InternetAddress.parse(recipient)[0];
                    } catch (AddressException e) {
                        log.error("Can't parse email address: address={}", recipient, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        try {
            sendEmail(sender, subject, body, session, recipientSet);
        } catch (SendFailedException e) {
            log.error("Error occurred while sending email", e);

            if (e.getInvalidAddresses() != null) {
                log.info("Trying to send email without invalid addresses: addresses={}", e.getInvalidAddresses().toString());
                retrySendEmailWithoutInvalidAddresses(sender, subject, body, session, recipientSet, e.getInvalidAddresses());
            } else {
                throw new RuntimeException(e);
            }
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    private static void sendEmail(
            String sender,
            String subject,
            String body,
            Session session,
            Set<Address> recipients
    ) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sender));

        message.setRecipients(Message.RecipientType.TO, recipients.toArray(new Address[recipients.size()]));
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
    }

    private static void retrySendEmailWithoutInvalidAddresses(String sender, String subject, String body, Session session,
                                                              Set<Address> recipients, Address[] invalidAddresses) {
        recipients.removeAll(Arrays.asList(invalidAddresses));
        try {
            sendEmail(sender, subject, body, session, recipients);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
