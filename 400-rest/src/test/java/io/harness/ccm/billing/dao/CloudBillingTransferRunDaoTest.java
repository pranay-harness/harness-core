/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.billing.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.commons.entities.billing.TransferJobRunState.PENDING;
import static io.harness.rule.OwnerRule.HANTANG;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ccm.commons.entities.billing.CloudBillingTransferRun;
import io.harness.ccm.commons.entities.billing.CloudBillingTransferRun.CloudBillingTransferRunKeys;
import io.harness.ccm.commons.entities.billing.TransferJobRunState;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CE)
public class CloudBillingTransferRunDaoTest extends WingsBaseTest {
  @Inject CloudBillingTransferRunDao cloudBillingTransferRunDao;
  private String accountId = "ACCOUNT_ID";
  private String billingDataPipelineRecordId = "BILLING_DATA_PIPELINE_RECORD_ID";
  private TransferJobRunState state = PENDING;
  private CloudBillingTransferRun cloudBillingTransferRun;

  @Before
  public void setUp() {
    cloudBillingTransferRun = CloudBillingTransferRun.builder()
                                  .accountId(accountId)
                                  .billingDataPipelineRecordId(billingDataPipelineRecordId)
                                  .state(state)
                                  .build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldUpsert() {
    CloudBillingTransferRun upserted = cloudBillingTransferRunDao.upsert(cloudBillingTransferRun);
    assertThat(upserted).isEqualToIgnoringGivenFields(
        cloudBillingTransferRun, CloudBillingTransferRunKeys.uuid, CloudBillingTransferRunKeys.lastUpdatedAt);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldList() {
    CloudBillingTransferRun upserted = cloudBillingTransferRunDao.upsert(cloudBillingTransferRun);
    List<CloudBillingTransferRun> runs = cloudBillingTransferRunDao.list(accountId, PENDING);
    assertThat(runs).contains(upserted);
  }
}
