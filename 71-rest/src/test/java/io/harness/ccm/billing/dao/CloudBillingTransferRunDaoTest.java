package io.harness.ccm.billing.dao;

import static io.harness.ccm.billing.entities.TransferJobRunState.PENDING;
import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.entities.CloudBillingTransferRun;
import io.harness.ccm.billing.entities.CloudBillingTransferRun.CloudBillingTransferRunKeys;
import io.harness.ccm.billing.entities.TransferJobRunState;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.List;

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
