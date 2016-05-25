package software.wings.helpers.ext.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.helpers.ext.mail.EmailData.Builder.anEmailData;
import static software.wings.helpers.ext.mail.SmtpConfig.Builder.aSmtpConfig;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import freemarker.template.TemplateException;
import org.apache.commons.mail.EmailException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import software.wings.WingsBaseTest;

import java.io.IOException;
import javax.mail.Address;
import javax.mail.MessagingException;

/**
 * Created by peeyushaggarwal on 5/20/16.
 */
public class MailerTest extends WingsBaseTest {
  private static final String EMAIL = "test@email.com";
  private static final String PASSWORD = "password";

  @Inject private Mailer mailer;
  @Rule public GreenMailRule greenMail = new GreenMailRule(ServerSetupTest.SMTP);

  @Before
  public void setup() {
    greenMail.setUser(EMAIL, EMAIL, PASSWORD);
  }

  @Test
  public void shouldSendNormalEmail() throws EmailException, TemplateException, IOException, MessagingException {
    mailer.send(aSmtpConfig()
                    .withHost("localhost")
                    .withPort(greenMail.getSmtp().getPort())
                    .withUsername(EMAIL)
                    .withPassword(PASSWORD)
                    .build(),
        anEmailData()
            .withFrom(EMAIL)
            .withBody("test")
            .withSubject("test")
            .withTo(Lists.newArrayList("recieve@email.com"))
            .build());

    assertThat(GreenMailUtil.getBody(greenMail.getReceivedMessages()[0])).isEqualTo("test");
    assertThat(greenMail.getReceivedMessages()[0].getSubject()).isEqualTo("test");
    assertThat(greenMail.getReceivedMessages()[0].getFrom()).extracting(Address::toString).containsExactly(EMAIL);
    assertThat(greenMail.getReceivedMessages()[0].getAllRecipients())
        .extracting(Address::toString)
        .containsExactly("recieve@email.com");
  }

  @Test
  public void shouldSendTemplatedEmail() throws EmailException, TemplateException, IOException, MessagingException {
    mailer.send(aSmtpConfig()
                    .withHost("localhost")
                    .withPort(greenMail.getSmtp().getPort())
                    .withUsername(EMAIL)
                    .withPassword(PASSWORD)
                    .build(),
        anEmailData()
            .withFrom(EMAIL)
            .withTemplateName("testmail")
            .withTemplateModel(ImmutableMap.of("name", "test"))
            .withTo(Lists.newArrayList("recieve@email.com"))
            .build());

    assertThat(GreenMailUtil.getBody(greenMail.getReceivedMessages()[0])).isEqualTo("hello test");
    assertThat(greenMail.getReceivedMessages()[0].getSubject()).isEqualTo("test you are invited");
    assertThat(greenMail.getReceivedMessages()[0].getFrom()).extracting(Address::toString).containsExactly(EMAIL);
    assertThat(greenMail.getReceivedMessages()[0].getAllRecipients())
        .extracting(Address::toString)
        .containsExactly("recieve@email.com");
  }
}
