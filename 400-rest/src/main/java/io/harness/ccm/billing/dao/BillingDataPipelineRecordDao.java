/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.billing.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.BillingDataPipelineRecord;
import io.harness.ccm.commons.entities.billing.BillingDataPipelineRecord.BillingDataPipelineRecordKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class BillingDataPipelineRecordDao {
  @Inject private HPersistence persistence;

  public boolean create(BillingDataPipelineRecord billingDataPipelineRecord) {
    return persistence.save(billingDataPipelineRecord) != null;
  }

  public BillingDataPipelineRecord get(String uuid) {
    return persistence.get(BillingDataPipelineRecord.class, uuid);
  }

  public BillingDataPipelineRecord fetchBillingPipelineMetaDataFromAccountId(String accountId) {
    return persistence.createQuery(BillingDataPipelineRecord.class)
        .filter(BillingDataPipelineRecordKeys.accountId, accountId)
        .get();
  }

  public List<BillingDataPipelineRecord> fetchBillingPipelineRecords(String accountId) {
    return persistence.createQuery(BillingDataPipelineRecord.class)
        .filter(BillingDataPipelineRecordKeys.accountId, accountId)
        .asList();
  }

  public BillingDataPipelineRecord fetchBillingPipelineRecord(String accountId, String settingId) {
    return persistence.createQuery(BillingDataPipelineRecord.class)
        .filter(BillingDataPipelineRecordKeys.accountId, accountId)
        .filter(BillingDataPipelineRecordKeys.settingId, settingId)
        .get();
  }
}
