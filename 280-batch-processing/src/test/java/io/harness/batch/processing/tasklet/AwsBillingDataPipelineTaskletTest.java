package io.harness.batch.processing.tasklet;

import static io.harness.batch.processing.ccm.CCMJobConstants.ACCOUNT_ID;
import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.preAggQueryKey;
import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.scheduledQueryKey;
import static io.harness.rule.OwnerRule.ROHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.entities.BillingDataPipelineRecord;
import io.harness.ccm.billing.entities.BillingDataPipelineRecord.BillingDataPipelineRecordKeys;
import io.harness.rule.Owner;

import software.wings.beans.Account;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.settings.SettingVariableTypes;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;

@RunWith(MockitoJUnitRunner.class)
public class AwsBillingDataPipelineTaskletTest extends CategoryTest {
  @Mock BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  @Mock BillingDataPipelineServiceImpl billingDataPipelineService;
  @Mock CloudToHarnessMappingService cloudToHarnessMappingService;

  @InjectMocks AwsBillingDataPipelineTasklet awsBillingDataPipelineTasklet;

  private static final String accountId = "accountId";
  private static final String accountName = "accountName";
  private static final String settingId = "settingId";
  private static final String masterAccountId = "masterAccountId";
  private static final String curReportName = "curReportName";
  private static final String dataSetId = "datasetId";
  private static final String transferJobName = "transferJobName";
  private static final Instant instant = Instant.now();
  private static final long startTime = instant.toEpochMilli();
  private static final long endTime = instant.plus(1, ChronoUnit.DAYS).toEpochMilli();
  private static final String scheduledQueryName = "scheduledQueryName";
  private static final String preAggQueryName = "preAggQueryName";
  private ChunkContext chunkContext;

  @Before
  public void setup() throws IOException {
    MockitoAnnotations.initMocks(this);
    Map<String, JobParameter> parameters = new HashMap<>();
    parameters.put(CCMJobConstants.JOB_START_DATE, new JobParameter(String.valueOf(startTime), true));
    parameters.put(CCMJobConstants.JOB_END_DATE, new JobParameter(String.valueOf(endTime), true));
    parameters.put(ACCOUNT_ID, new JobParameter(ACCOUNT_ID, true));
    JobParameters jobParameters = new JobParameters(parameters);
    StepExecution stepExecution = new StepExecution("awsBillingDataPipelineStep", new JobExecution(0L, jobParameters));
    chunkContext = new ChunkContext(new StepContext(stepExecution));

    SettingAttribute settingAttribute =
        SettingAttribute.Builder.aSettingAttribute()
            .withUuid(settingId)
            .withValue(CEAwsConfig.builder().curReportName(curReportName).awsMasterAccountId(masterAccountId).build())
            .build();
    when(cloudToHarnessMappingService.getAccountInfoFromId(accountId))
        .thenReturn(Account.Builder.anAccount().withAccountName(accountName).build());
    when(cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(
             accountId, SettingAttribute.SettingCategory.CE_CONNECTOR, SettingVariableTypes.CE_AWS, startTime, endTime))
        .thenReturn(Collections.singletonList(settingAttribute));
    when(billingDataPipelineService.createDataSet(any())).thenReturn(dataSetId);
    when(billingDataPipelineService.createDataTransferJobFromGCS(
             dataSetId, settingId, accountId, accountName, curReportName, false))
        .thenReturn(transferJobName);
    when(billingDataPipelineService.createDataTransferJobFromGCS(
             dataSetId, settingId, accountId, accountName, curReportName, true))
        .thenReturn(transferJobName);
    HashMap<String, String> scheduledQueryJobsMap = new HashMap<>();
    scheduledQueryJobsMap.put(scheduledQueryKey, scheduledQueryName);
    scheduledQueryJobsMap.put(preAggQueryKey, preAggQueryName);
    when(billingDataPipelineService.createScheduledQueriesForAWS(dataSetId, accountId, accountName))
        .thenReturn(scheduledQueryJobsMap);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldExecute() throws Exception {
    awsBillingDataPipelineTasklet.execute(null, chunkContext);
    ArgumentCaptor<BillingDataPipelineRecord> billingDataPipelineRecordArgumentCaptor =
        ArgumentCaptor.forClass(BillingDataPipelineRecord.class);
    verify(billingDataPipelineRecordDao).create(billingDataPipelineRecordArgumentCaptor.capture());
    BillingDataPipelineRecord value = billingDataPipelineRecordArgumentCaptor.getValue();
    BillingDataPipelineRecord expectedRecord = BillingDataPipelineRecord.builder()
                                                   .accountId(accountId)
                                                   .accountName(accountName)
                                                   .settingId(settingId)
                                                   .cloudProvider("AWS")
                                                   .dataSetId(dataSetId)
                                                   .awsMasterAccountId(masterAccountId)
                                                   .dataTransferJobName(transferJobName)
                                                   .awsFallbackTableScheduledQueryName(scheduledQueryName)
                                                   .preAggregatedScheduledQueryName(preAggQueryName)
                                                   .build();
    assertThat(value).isEqualToIgnoringGivenFields(
        expectedRecord, BillingDataPipelineRecordKeys.uuid, BillingDataPipelineRecordKeys.createdAt);
  }
}
