package io.harness.batch.processing.dao.impl;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord.BillingDataPipelineRecordKeys;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.WingsBaseTest;

@RunWith(MockitoJUnitRunner.class)
public class BillingDataPipelineRecordDaoImplTest extends WingsBaseTest {
  @Inject private BillingDataPipelineRecordDaoImpl billingDataPipelineRecordDao;
  @Inject private HPersistence hPersistence;

  private final String accountId = "accountId_" + this.getClass().getSimpleName();
  private final String accountName = "accountName_" + this.getClass().getSimpleName();
  private final String settingId = "settingId_" + this.getClass().getSimpleName();
  private final String dataSetId = "dataSetId_" + this.getClass().getSimpleName();
  private final String dataTransferJobName = "dataTransferJobName_" + this.getClass().getSimpleName();
  private final String fallBackTableName = "fallBackTableName_" + this.getClass().getSimpleName();
  private final String preAggTableName = "preAggTableName_" + this.getClass().getSimpleName();

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void create() {
    BillingDataPipelineRecord dataPipelineRecord = BillingDataPipelineRecord.builder()
                                                       .accountId(accountId)
                                                       .accountName(accountName)
                                                       .settingId(settingId)
                                                       .dataSetId(dataSetId)
                                                       .dataTransferJobName(dataTransferJobName)
                                                       .fallbackTableScheduledQueryName(fallBackTableName)
                                                       .preAggregatedScheduledQueryName(preAggTableName)
                                                       .build();
    billingDataPipelineRecordDao.create(dataPipelineRecord);

    BillingDataPipelineRecord billingDataPipelineRecord =
        hPersistence.createQuery(BillingDataPipelineRecord.class)
            .filter(BillingDataPipelineRecordKeys.accountId, accountId)
            .filter(BillingDataPipelineRecordKeys.dataSetId, dataSetId)
            .get();

    assertThat(billingDataPipelineRecord).isEqualTo(dataPipelineRecord);
  }
}