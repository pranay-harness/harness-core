package io.harness.ng.core.invites.ext.mail;

import com.google.inject.Inject;

import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

public class EmailNotificationListener extends QueueListener<EmailData> {
  @Inject private MailUtils mailUtils;

  @Inject
  public EmailNotificationListener(QueueConsumer<EmailData> queueConsumer) {
    super(queueConsumer, true);
  }

  @Override
  public void onMessage(EmailData message) {
    mailUtils.sendMailAsyncConsumer(message);
  }
}
