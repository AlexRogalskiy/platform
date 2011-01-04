package platform.server;


import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.util.Properties;

public class EmailSender {
    String smtpHost = "169.254.1.6";
    String fromAddress = "luxsoft@adsl.by";
    Session mailSession;
    MimeMessage message;
    Multipart mp = new MimeMultipart();
    String emails[];

    public EmailSender(String... targets) {
        emails = targets;

        Properties mailProps = new Properties();
        mailProps.setProperty("mail.smtp.host", smtpHost);
        mailProps.put("mail.from", fromAddress);
        mailSession = Session.getInstance(mailProps, null);
        
        message = new MimeMessage(mailSession);
    }

    public void setRecipients(String... targets) throws MessagingException {
        InternetAddress dests[] = new InternetAddress[targets.length];
        for (int i = 0; i < targets.length; i++) {
            dests[i] = new InternetAddress(targets[i].trim().toLowerCase());
        }
        message.setRecipients(MimeMessage.RecipientType.TO, dests);
    }

    public void setText(String text) throws MessagingException {
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setDataHandler((new DataHandler(new HTMLDataSource(text))));
        mp.addBodyPart(textPart);
    }

    static class HTMLDataSource implements DataSource {
        private String html;

        public HTMLDataSource(String htmlString) {
            html = htmlString;
        }

        public InputStream getInputStream() throws IOException {
            if (html == null) throw new IOException("null html");
            return new ByteArrayInputStream(html.getBytes());
        }

        public OutputStream getOutputStream() throws IOException {
            throw new IOException("HTMLDataHandler cannot write HTML");
        }

        public String getContentType() {
            return "text/html";
        }

        public String getName() {
            return "dataSource to send text/html";
        }
    }

    public void attachFile(String path) throws MessagingException {
        MimeBodyPart filePart = new MimeBodyPart();
        FileDataSource fds = new FileDataSource(path);
        filePart.setDataHandler(new DataHandler(fds));
        filePart.setFileName(fds.getName());
        mp.addBodyPart(filePart);
    }

    public void sendMail(String subject, String htmlFilePath, String... filePaths) {
        try {
            message.setFrom();
            message.setSentDate(new java.util.Date());
            setRecipients(emails);
            message.setSubject(subject);

            String result = "";
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(htmlFilePath)));
            while (in.ready()) {
                String s = in.readLine();
                result += s;
            }

            setText(result);
            for (String path : filePaths) {
                attachFile(path);
            }
            message.setContent(mp);

            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
