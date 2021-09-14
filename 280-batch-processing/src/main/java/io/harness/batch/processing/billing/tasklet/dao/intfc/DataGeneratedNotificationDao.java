/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.billing.tasklet.dao.intfc;

import io.harness.ccm.commons.entities.batch.DataGeneratedNotification;

public interface DataGeneratedNotificationDao {
  boolean save(DataGeneratedNotification notification);
  boolean isMailSent(String accountId);
}
