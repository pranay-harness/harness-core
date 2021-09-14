/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.polling.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.polling.bean.PolledResponse;
import io.harness.polling.bean.PollingDocument;
import io.harness.polling.contracts.PollingItem;

import javax.validation.Valid;

@OwnedBy(HarnessTeam.CDC)
public interface PollingService {
  String save(@Valid PollingDocument pollingDocument);

  PollingDocument get(String accountId, String pollingDocId);

  void delete(PollingDocument pollingDocument);

  boolean attachPerpetualTask(String accountId, String pollDocId, String perpetualTaskId);

  void updateFailedAttempts(String accountId, String pollingDocId, int failedAttempts);

  void updatePolledResponse(String accountId, String pollingDocId, PolledResponse polledResponse);

  String subscribe(PollingItem pollingItem) throws InvalidRequestException;

  boolean unsubscribe(PollingItem pollingItem);
}
