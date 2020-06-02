package io.harness.batch.processing.service.impl;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.bigquery.datatransfer.v1.TransferState;

import io.harness.CategoryTest;
import io.harness.batch.processing.dao.intfc.BatchJobScheduledDataDao;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

public class BillingDataPipelineHealthStatusServiceImplTest extends CategoryTest {
  @InjectMocks BillingDataPipelineHealthStatusServiceImpl billingDataPipelineHealthStatusService;
  @Mock BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  @Mock BatchJobScheduledDataDao batchJobScheduledDataDao;

  private static final String displayName = "displayName";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    BillingDataPipelineRecord billingDataPipelineRecord = BillingDataPipelineRecord.builder().build();
    when(billingDataPipelineRecordDao.getAllBillingDataPipelineRecords())
        .thenReturn(Collections.singletonList(billingDataPipelineRecord));
    when(batchJobScheduledDataDao.fetchLastBatchJobScheduledData(anyString(), any())).thenReturn(null);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void computeTransferConfigStatusMap() {
    String value = billingDataPipelineHealthStatusService.getTransferStateStringValue(
        Collections.singletonMap(displayName, TransferState.SUCCEEDED), displayName);
    assertThat(value).isEqualTo(TransferState.SUCCEEDED.toString());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void updateBillingPipelineRecordsStatus() {
    billingDataPipelineHealthStatusService.updateBillingPipelineRecordsStatus(Collections.EMPTY_MAP);
    ArgumentCaptor<BillingDataPipelineRecord> billingDataPipelineRecordArgumentCaptor =
        ArgumentCaptor.forClass(BillingDataPipelineRecord.class);
    verify(billingDataPipelineRecordDao).upsert(billingDataPipelineRecordArgumentCaptor.capture());
    BillingDataPipelineRecord billingDataPipelineRecord = billingDataPipelineRecordArgumentCaptor.getValue();
    assertThat(billingDataPipelineRecord.getDataTransferJobStatus()).isEqualTo(TransferState.UNRECOGNIZED.toString());
  }
}