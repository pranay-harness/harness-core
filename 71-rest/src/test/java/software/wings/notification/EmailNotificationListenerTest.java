package software.wings.notification;

import static io.harness.rule.OwnerRule.ANUBHAW;
import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;

/**
 * Created by peeyushaggarwal on 5/25/16.
 */
public class EmailNotificationListenerTest extends WingsBaseTest {
  private static final EmailData testEmailData = EmailData.builder().build();
  @Mock private EmailNotificationService emailNotificationService;

  @InjectMocks @Inject private EmailNotificationListener emailNotificationListener;

  /**
   * Should send email on receiving message.
   *
   * @throws Exception the exception
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldSendEmailOnReceivingMessage() throws Exception {
    emailNotificationListener.onMessage(testEmailData);
    verify(emailNotificationService).send(testEmailData);
  }
}
