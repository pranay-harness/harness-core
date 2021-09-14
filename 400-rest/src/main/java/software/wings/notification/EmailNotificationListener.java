/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.notification;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.queue.QueueConsumer;
import io.harness.queue.QueueListener;

import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.intfc.EmailNotificationService;

import com.google.inject.Inject;

/**
 * Created by peeyushaggarwal on 5/24/16.
 *
 * @see EmailData
 */
@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._830_NOTIFICATION_SERVICE)
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
