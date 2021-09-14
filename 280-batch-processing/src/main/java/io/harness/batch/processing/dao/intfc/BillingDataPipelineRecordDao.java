/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.batch.processing.dao.intfc;

import io.harness.ccm.commons.entities.billing.BillingDataPipelineRecord;

import java.util.List;

public interface BillingDataPipelineRecordDao {
  String create(BillingDataPipelineRecord billingDataPipelineRecord);

  BillingDataPipelineRecord getByMasterAccountId(String accountId, String awsMasterAccountId);

  BillingDataPipelineRecord getByAccountId(String accountId);

  List<BillingDataPipelineRecord> getAllRecordsByAccountId(String accountId);

  BillingDataPipelineRecord getBySettingId(String accountId, String settingId);

  List<BillingDataPipelineRecord> listByGcpBillingAccountDataset(
      String accountId, String gcpBqProjectId, String gcpBqDatasetId);

  List<BillingDataPipelineRecord> listAllBillingDataPipelineRecords();

  BillingDataPipelineRecord upsert(BillingDataPipelineRecord billingDataPipelineRecord);

  boolean removeBillingDataPipelineRecord(String accountId, String settingId);
}
