package software.wings.service;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static org.junit.Assert.assertEquals;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import software.wings.helpers.ext.mail.EmailData;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * For every email action, this class tests if the template matches to the expected output.
 */
public class OutgoingEmailTest {
  @Test
  public void testEmailTemplates() throws Exception {
    Configuration cfg = new Configuration(VERSION_2_3_23);
    cfg.setClassForTemplateLoading(getClass(), "/mailtemplates");

    Map testData = new HashMap();
    testData.put("company", "Testcompany");
    testData.put("name", "Testname");
    testData.put("url", "TestURL");
    testData.put("totpSecret", "testTotpSecret");

    EmailData emailData = EmailData.builder().templateModel(testData).build();

    verifyMailForAction(cfg, emailData, "invite");
    verifyMailForAction(cfg, emailData, "add_account");
    verifyMailForAction(cfg, emailData, "add_role");
    verifyMailForAction(cfg, emailData, "reset_password");
    verifyMailForAction(cfg, emailData, "signup");
    verifyMailForAction(cfg, emailData, "reset_2fa");
  }

  private void verifyMailForAction(Configuration cfg, EmailData emailData, String action)
      throws IOException, TemplateException {
    Template subjectTemplate = getSubjectTemplate(cfg, action);
    Template plainBodyTemplate = getBodyPlainTextTemplate(cfg, action);
    StringWriter subjectWriter = new StringWriter();
    subjectTemplate.process(emailData.getTemplateModel(), subjectWriter);
    String subject = subjectWriter.toString();
    String expectedSubject =
        IOUtils.toString(getClass().getResourceAsStream("/mailverification/" + action + "-subject.txt"), "UTF-8");
    assertEquals("Subject do not match for action=" + action, expectedSubject, subject);

    StringWriter bodyWriter = new StringWriter();
    plainBodyTemplate.process(emailData.getTemplateModel(), bodyWriter);
    String body = bodyWriter.toString();
    String expectedBody = IOUtils.toString(
        getClass().getResourceAsStream("/mailverification/" + action + "-body_plain_text.txt"), "UTF-8");
    assertEquals("Body does not match for action=" + action, expectedBody, body);
  }

  private Template getSubjectTemplate(Configuration configuration, String action) throws IOException {
    return configuration.getTemplate(action + "-subject.ftl");
  }

  private Template getBodyPlainTextTemplate(Configuration configuration, String action) throws IOException {
    return configuration.getTemplate(action + "-body_plain_text.ftl");
  }
}
