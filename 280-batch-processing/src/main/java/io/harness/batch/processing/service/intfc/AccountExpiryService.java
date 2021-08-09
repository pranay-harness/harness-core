package io.harness.batch.processing.service.intfc;

import io.harness.ccm.commons.entities.billing.BillingDataPipelineRecord;

import software.wings.beans.Account;

public interface AccountExpiryService {
  boolean dataPipelineCleanup(Account account);
  void deletePipelinePerRecord(String accountId, BillingDataPipelineRecord billingDataPipelineRecord);
}
