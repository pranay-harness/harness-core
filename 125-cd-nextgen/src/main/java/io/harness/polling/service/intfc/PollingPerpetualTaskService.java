/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.polling.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.polling.bean.PollingDocument;

@OwnedBy(HarnessTeam.CDC)
public interface PollingPerpetualTaskService {
  void createPerpetualTask(PollingDocument pollingDocument);
  void resetPerpetualTask(PollingDocument pollingDocument);
  void deletePerpetualTask(String perpetualTaskId, String accountId);
}
