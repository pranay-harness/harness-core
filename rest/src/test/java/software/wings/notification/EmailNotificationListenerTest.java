package software.wings.notification;

import static org.mockito.Mockito.verify;

import com.google.inject.Inject;

import org.junit.Test;
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
  public void shouldSendEmailOnReceivingMessage() throws Exception {
    emailNotificationListener.onMessage(testEmailData);
    verify(emailNotificationService).send(testEmailData);
  }
}
