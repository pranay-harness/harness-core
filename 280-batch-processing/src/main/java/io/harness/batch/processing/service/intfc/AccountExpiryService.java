/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.service.intfc;

import io.harness.ccm.commons.entities.billing.BillingDataPipelineRecord;

import software.wings.beans.Account;

public interface AccountExpiryService {
  boolean dataPipelineCleanup(Account account);
  void deletePipelinePerRecord(String accountId, BillingDataPipelineRecord billingDataPipelineRecord);
}
