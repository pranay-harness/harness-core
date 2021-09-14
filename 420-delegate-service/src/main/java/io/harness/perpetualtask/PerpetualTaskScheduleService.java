/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.perpetualtask;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.perpetualtask.PerpetualTaskScheduleConfig;

@OwnedBy(HarnessTeam.DEL)
public interface PerpetualTaskScheduleService {
  PerpetualTaskScheduleConfig save(String accountId, String perpetualTaskType, long timeIntervalInMillis);

  PerpetualTaskScheduleConfig getByAccountIdAndPerpetualTaskType(String accountId, String perpetualTaskType);

  boolean resetByAccountIdAndPerpetualTaskType(String accountId, String perpetualTaskType, long timeIntervalInMillis);
}
