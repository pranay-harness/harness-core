/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.cluster.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.rule.OwnerRule.ROHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.dao.BillingDataPipelineRecordDao;
import io.harness.ccm.commons.entities.billing.BillingDataPipelineRecord;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CE)
public class BillingDataPipelineRecordDaoTest extends WingsBaseTest {
  @Inject BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  private static final String accountId = "ACCOUNT_ID";
  private static final String settingId = "SETTING_ID";
  private static final String dataSetId = "DATASET_ID";
  private static BillingDataPipelineRecord billingDataPipelineRecord;

  @Before
  public void setUp() {
    billingDataPipelineRecord =
        BillingDataPipelineRecord.builder().accountId(accountId).settingId(settingId).dataSetId(dataSetId).build();
    billingDataPipelineRecordDao.create(billingDataPipelineRecord);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void fetchBillingPipelineMetaDataFromAccountId() {
    BillingDataPipelineRecord billingDataPipelineRecord =
        billingDataPipelineRecordDao.fetchBillingPipelineMetaDataFromAccountId(accountId);
    assertThat(billingDataPipelineRecord.getDataSetId()).isEqualTo(dataSetId);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void fetchBillingPipelineRecords() {
    List<BillingDataPipelineRecord> billingDataPipelineRecords =
        billingDataPipelineRecordDao.fetchBillingPipelineRecords(accountId);
    assertThat(billingDataPipelineRecords.get(0).getDataSetId()).isEqualTo(dataSetId);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void fetchBillingPipelineRecord() {
    BillingDataPipelineRecord billingDataPipelineRecord =
        billingDataPipelineRecordDao.fetchBillingPipelineRecord(accountId, settingId);
    assertThat(billingDataPipelineRecord.getDataSetId()).isEqualTo(dataSetId);
  }
}
