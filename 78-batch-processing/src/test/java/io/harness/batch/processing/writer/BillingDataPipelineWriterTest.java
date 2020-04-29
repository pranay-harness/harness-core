package io.harness.batch.processing.writer;

import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.preAggQueryKey;
import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.scheduledQueryKey;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.core.JobParameters;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.settings.SettingValue;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;

@RunWith(MockitoJUnitRunner.class)
public class BillingDataPipelineWriterTest extends CategoryTest {
  @InjectMocks BillingDataPipelineWriter billingDataPipelineWriter;
  @Mock BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  @Mock BillingDataPipelineServiceImpl billingDataPipelineService;
  @Mock CloudToHarnessMappingService cloudToHarnessMappingService;
  @Mock JobParameters parameters;

  private static final String settingId = "settingId";
  private static final String accountId = "accountId";
  private static final String accountName = "accountName";
  private static final String dataSetId = "datasetId";
  private static final String transferJobName = "transferJobName";
  private static final Instant instant = Instant.now();
  private static final long startTime = instant.toEpochMilli();
  private static final long endTime = instant.plus(1, ChronoUnit.DAYS).toEpochMilli();
  private static final String scheduledQueryName = "scheduledQueryName";
  private static final String preAggQueryName = "preAggQueryName";

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    SettingAttribute settingAttribute = new SettingAttribute();
    settingAttribute.setUuid(settingId);
    when(parameters.getString(CCMJobConstants.ACCOUNT_ID)).thenReturn(accountId);
    when(parameters.getLong(CCMJobConstants.JOB_START_DATE)).thenReturn(startTime);
    when(parameters.getLong(CCMJobConstants.JOB_END_DATE)).thenReturn(endTime);

    when(cloudToHarnessMappingService.getAccountNameFromId(accountId)).thenReturn(accountName);
    when(cloudToHarnessMappingService.getSettingAttributes(accountId,
             SettingAttribute.SettingCategory.CE_CONNECTOR.toString(),
             SettingValue.SettingVariableTypes.CE_AWS.toString(), startTime, endTime))
        .thenReturn(Collections.singletonList(settingAttribute));
    when(billingDataPipelineService.createDataSet(accountId, accountName)).thenReturn(dataSetId);
    when(billingDataPipelineService.createDataTransferJob(dataSetId, settingId, accountId, accountName))
        .thenReturn(transferJobName);
    HashMap<String, String> scheduledQueryJobsMap = new HashMap<>();
    scheduledQueryJobsMap.put(scheduledQueryKey, scheduledQueryName);
    scheduledQueryJobsMap.put(preAggQueryKey, preAggQueryName);
    when(billingDataPipelineService.createScheduledQueries(dataSetId, accountId, accountName))
        .thenReturn(scheduledQueryJobsMap);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testWrite() {
    billingDataPipelineWriter.write(Collections.EMPTY_LIST);
    ArgumentCaptor<BillingDataPipelineRecord> billingDataPipelineRecordArgumentCaptor =
        ArgumentCaptor.forClass(BillingDataPipelineRecord.class);
    verify(billingDataPipelineRecordDao).create(billingDataPipelineRecordArgumentCaptor.capture());
    BillingDataPipelineRecord value = billingDataPipelineRecordArgumentCaptor.getValue();
    assertThat(value.getAccountId()).isEqualTo(accountId);
    assertThat(value.getAccountName()).isEqualTo(accountName);
    assertThat(value.getSettingId()).isEqualTo(settingId);
    assertThat(value.getDataTransferJobName()).isEqualTo(transferJobName);
    assertThat(value.getFallbackTableScheduledQueryName()).isEqualTo(scheduledQueryName);
    assertThat(value.getPreAggregatedScheduledQueryName()).isEqualTo(preAggQueryName);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testWriteTransferJobThrowsException() throws IOException {
    when(billingDataPipelineService.createDataTransferJob(dataSetId, settingId, accountId, accountName))
        .thenThrow(IOException.class);
    billingDataPipelineWriter.write(Collections.EMPTY_LIST);
    ArgumentCaptor<BillingDataPipelineRecord> billingDataPipelineRecordArgumentCaptor =
        ArgumentCaptor.forClass(BillingDataPipelineRecord.class);
    verify(billingDataPipelineRecordDao).create(billingDataPipelineRecordArgumentCaptor.capture());
    BillingDataPipelineRecord value = billingDataPipelineRecordArgumentCaptor.getValue();
    assertThat(value.getDataTransferJobName()).isNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testScheduledQueriesThrowsException() throws IOException {
    when(billingDataPipelineService.createScheduledQueries(dataSetId, accountId, accountName))
        .thenThrow(IOException.class);
    billingDataPipelineWriter.write(Collections.EMPTY_LIST);
    ArgumentCaptor<BillingDataPipelineRecord> billingDataPipelineRecordArgumentCaptor =
        ArgumentCaptor.forClass(BillingDataPipelineRecord.class);
    verify(billingDataPipelineRecordDao).create(billingDataPipelineRecordArgumentCaptor.capture());
    BillingDataPipelineRecord value = billingDataPipelineRecordArgumentCaptor.getValue();
    assertThat(value.getPreAggregatedScheduledQueryName()).isNull();
    assertThat(value.getFallbackTableScheduledQueryName()).isNull();
  }
}