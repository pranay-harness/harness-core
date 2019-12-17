package software.wings.notification;

import com.google.inject.Inject;

import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;

/**
 * Created by peeyushaggarwal on 5/24/16.
 *
 * @see EmailData
 */
public class EmailNotificationListener extends QueueListener<EmailData> {
  @Inject private EmailNotificationService emailNotificationService;

  @Inject
  public EmailNotificationListener(QueueConsumer<EmailData> queueConsumer) {
    super(queueConsumer, true);
  }

  /* (non-Javadoc)
   * @see software.wings.core.queue.QueueListener#onMessage(software.wings.core.queue.Queuable)
   */
  @Override
  public void onMessage(EmailData message) {
    emailNotificationService.send(message);
  }
}
