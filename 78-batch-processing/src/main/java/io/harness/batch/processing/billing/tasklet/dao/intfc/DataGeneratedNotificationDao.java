package io.harness.batch.processing.billing.tasklet.dao.intfc;

import io.harness.batch.processing.billing.tasklet.entities.DataGeneratedNotification;

public interface DataGeneratedNotificationDao {
  boolean save(DataGeneratedNotification notification);
  boolean isMailSent(String accountId);
}
