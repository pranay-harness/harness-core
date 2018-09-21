package software.wings.helpers.ext.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.common.Constants.HARNESS_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import freemarker.template.TemplateException;
import io.harness.rule.AuthorRule.Author;
import org.apache.commons.mail.EmailException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.wings.WingsBaseTest;

import java.io.IOException;
import java.util.Collections;
import javax.mail.Address;
import javax.mail.MessagingException;

/**
 * Created by peeyushaggarwal on 5/20/16.
 */
public class MailerTest extends WingsBaseTest {
  private static final String EMAIL = "test@email.com";
  private static final char[] PASSWORD = "password".toCharArray();
  /**
   * The Green mail.
   */
  @Rule public GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);
  @Inject private Mailer mailer;

  /**
   * Setup.
   */
  @Before
  public void setup() {
    greenMail.setUser(EMAIL, EMAIL, new String(PASSWORD));
  }

  /**
   * Should send normal email.
   *
   * @throws EmailException     the email exception
   * @throws TemplateException  the template exception
   * @throws IOException        Signals that an I/O exception has occurred.
   * @throws MessagingException the messaging exception
   */
  @Test
  public void shouldSendNormalEmail() throws MessagingException {
    mailer.send(SmtpConfig.builder()
                    .fromAddress(EMAIL)
                    .host("localhost")
                    .port(greenMail.getSmtp().getPort())
                    .username(EMAIL)
                    .password(PASSWORD)
                    .build(),
        Collections.emptyList(),
        EmailData.builder()
            .hasHtml(false)
            .body("test")
            .subject("test")
            .to(Lists.newArrayList("recieve@email.com"))
            .cc(Collections.emptyList())
            .build());

    assertThat(GreenMailUtil.getBody(greenMail.getReceivedMessages()[0])).isEqualTo("test");
    assertThat(greenMail.getReceivedMessages()[0].getSubject()).isEqualTo("test");
    assertThat(greenMail.getReceivedMessages()[0].getFrom())
        .extracting(Address::toString)
        .containsExactly(HARNESS_NAME + " <" + EMAIL + ">");
    assertThat(greenMail.getReceivedMessages()[0].getReplyTo()[0].toString()).isEqualTo(EMAIL);
    assertThat(greenMail.getReceivedMessages()[0].getAllRecipients())
        .extracting(Address::toString)
        .containsExactly("recieve@email.com");
  }

  /**
   * Should send html email.
   *
   * @throws MessagingException the messaging exception
   * @throws EmailException     the email exception
   * @throws TemplateException  the template exception
   * @throws IOException        the io exception
   */
  @Test
  @Author(email = "raghu@harness.io", intermittent = true)
  public void shouldSendHtmlEmail() throws MessagingException {
    mailer.send(SmtpConfig.builder()
                    .fromAddress(EMAIL)
                    .host("localhost")
                    .port(greenMail.getSmtp().getPort())
                    .username(EMAIL)
                    .password(PASSWORD)
                    .build(),
        Collections.emptyList(),
        EmailData.builder()
            .hasHtml(true)
            .body("test")
            .subject("test")
            .to(Lists.newArrayList("recieve@email.com"))
            .build());

    assertThat(GreenMailUtil.getBody(greenMail.getReceivedMessages()[0])).contains("Content-Type: text/html;");
    assertThat(greenMail.getReceivedMessages()[0].getSubject()).isEqualTo("test");
    assertThat(greenMail.getReceivedMessages()[0].getFrom())
        .extracting(Address::toString)
        .containsExactly(HARNESS_NAME + " <" + EMAIL + ">");
    assertThat(greenMail.getReceivedMessages()[0].getReplyTo()[0].toString()).isEqualTo(EMAIL);
    assertThat(greenMail.getReceivedMessages()[0].getAllRecipients())
        .extracting(Address::toString)
        .containsExactly("recieve@email.com");
  }

  /**
   * Should send templated email.
   *
   * @throws EmailException     the email exception
   * @throws TemplateException  the template exception
   * @throws IOException        Signals that an I/O exception has occurred.
   * @throws MessagingException the messaging exception
   */
  @Test
  public void shouldSendTemplatedEmail() throws MessagingException {
    mailer.send(SmtpConfig.builder()
                    .fromAddress(EMAIL)
                    .host("localhost")
                    .port(greenMail.getSmtp().getPort())
                    .username(EMAIL)
                    .password(PASSWORD)
                    .build(),
        Collections.emptyList(),
        EmailData.builder()
            .hasHtml(false)
            .templateName("testmail")
            .templateModel(ImmutableMap.of("name", "test"))
            .to(Lists.newArrayList("recieve@email.com"))
            .cc(Lists.newArrayList("recieve2@email.com"))
            .build());

    assertThat(GreenMailUtil.getBody(greenMail.getReceivedMessages()[0])).isEqualTo("hello test");
    assertThat(greenMail.getReceivedMessages()[0].getSubject()).isEqualTo("test you are invited");
    assertThat(greenMail.getReceivedMessages()[0].getFrom())
        .extracting(Address::toString)
        .containsExactly(HARNESS_NAME + " <" + EMAIL + ">");
    assertThat(greenMail.getReceivedMessages()[0].getReplyTo()[0].toString()).isEqualTo(EMAIL);
    assertThat(greenMail.getReceivedMessages()[0].getAllRecipients())
        .extracting(Address::toString)
        .containsExactly("recieve@email.com", "recieve2@email.com");
  }
}
