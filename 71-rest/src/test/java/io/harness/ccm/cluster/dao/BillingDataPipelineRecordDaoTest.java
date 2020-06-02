package io.harness.ccm.cluster.dao;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

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
  public void fetchBillingPipelineRecord() {
    BillingDataPipelineRecord billingDataPipelineRecord =
        billingDataPipelineRecordDao.fetchBillingPipelineRecord(accountId, settingId);
    assertThat(billingDataPipelineRecord.getDataSetId()).isEqualTo(dataSetId);
  }
}