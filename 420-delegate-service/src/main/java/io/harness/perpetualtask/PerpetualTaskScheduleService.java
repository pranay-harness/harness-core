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
