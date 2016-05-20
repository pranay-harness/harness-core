package software.wings.helpers.ext;

import static freemarker.template.Configuration.VERSION_2_3_23;

import com.google.common.collect.ImmutableMap;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Created by peeyushaggarwal on 5/20/16.
 */
public class Mailer {
  private static final Configuration cfg = new Configuration(VERSION_2_3_23);

  public static void main1(String[] args) throws EmailException {
    Email email = new SimpleEmail();
    email.setHostName("smtp.gmail.com");
    email.setSmtpPort(465);
    email.setAuthenticator(new DefaultAuthenticator("wings_test@wings.software", "@wes0me@pp"));
    email.setSSLOnConnect(true);

    email.setFrom("wings_test@wings.software");
    email.setSubject("TestMail");
    email.setMsg("This is a test mail ... :-)");
    email.addTo("peeyush@wings.software");

    email.send();
  }

  public static void main2(String[] args) throws EmailException, IOException, TemplateException {
    Email email = new SimpleEmail();
    email.setHostName("smtp.gmail.com");
    email.setSmtpPort(465);
    email.setAuthenticator(new DefaultAuthenticator("wings_test@wings.software", "@wes0me@pp"));
    email.setSSLOnConnect(true);

    StringWriter stringWriter = new StringWriter();
    Template t = new Template("name", new StringReader("hello ${test}"), cfg);
    t.process(ImmutableMap.of("test", "user"), stringWriter);
    email.setFrom("wings_test@wings.software");
    email.setSubject("TestMail");
    email.setMsg(stringWriter.toString());
    email.addTo("peeyush@wings.software");

    email.send();
  }
}
