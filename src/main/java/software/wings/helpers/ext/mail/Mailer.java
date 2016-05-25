package software.wings.helpers.ext.mail;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Created by peeyushaggarwal on 5/20/16.
 */
public class Mailer {
  private final Configuration cfg = new Configuration(VERSION_2_3_23);

  public Mailer() {
    cfg.setTemplateLoader(new ClassTemplateLoader(Mailer.class, "/mailtemplates"));
  }

  public void send(SmtpConfig smtpConfig, EmailData emailData) throws EmailException, IOException, TemplateException {
    Email email = new SimpleEmail();
    email.setHostName(smtpConfig.getHost());
    email.setSmtpPort(smtpConfig.getPort());
    email.setAuthenticator(new DefaultAuthenticator(smtpConfig.getUsername(), smtpConfig.getPassword()));
    email.setSSLOnConnect(smtpConfig.isUseSSL());
    if (smtpConfig.isUseSSL()) {
      email.setSslSmtpPort(Integer.toString(smtpConfig.getPort()));
    }

    email.setFrom(emailData.getFrom());
    for (String to : emailData.getTo()) {
      email.addTo(to);
    }

    String subject = emailData.getSubject();
    String body = emailData.getBody();
    if (isNotBlank(emailData.getTemplateName())) {
      Template subjectTemplate = cfg.getTemplate(emailData.getTemplateName() + "-subject.ftl");
      Template bodyTemplate = cfg.getTemplate(emailData.getTemplateName() + "-body.ftl");

      StringWriter subjectWriter = new StringWriter();
      subjectTemplate.process(emailData.getTemplateModel(), subjectWriter);

      subject = subjectWriter.toString();

      StringWriter bodyWriter = new StringWriter();
      bodyTemplate.process(emailData.getTemplateModel(), bodyWriter);

      body = bodyWriter.toString();
    }

    email.setSubject(subject);
    email.setMsg(body);

    email.send();
  }
}
