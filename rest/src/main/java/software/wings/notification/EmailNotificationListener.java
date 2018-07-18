package software.wings.notification;

import com.google.inject.Inject;

import software.wings.core.queue.AbstractQueueListener;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;

/**
 * Created by peeyushaggarwal on 5/24/16.
 *
 * @see EmailData
 */
public class EmailNotificationListener extends AbstractQueueListener<EmailData> {
  @Inject private EmailNotificationService emailNotificationService;

  public EmailNotificationListener() {
    super(true);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.AbstractQueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  protected void onMessage(EmailData message) {
    emailNotificationService.send(message);
  }
}
