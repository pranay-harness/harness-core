package io.harness.batch.processing.billing.tasklet.dao.intfc;

import io.harness.ccm.commons.entities.batch.DataGeneratedNotification;

public interface DataGeneratedNotificationDao {
  boolean save(DataGeneratedNotification notification);
  boolean isMailSent(String accountId);
}
