package software.wings.notification;

import com.google.inject.Inject;

import software.wings.core.queue.AbstractQueueListener;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.NotificationService;

/**
 * Created by peeyushaggarwal on 5/24/16.
 */
public class EmailNotificationListener extends AbstractQueueListener<EmailData> {
  @Inject private NotificationService<EmailData> emailNotificationService;

  @Override
  protected void onMessage(EmailData message) throws Exception {
    emailNotificationService.send(message);
  }
}
