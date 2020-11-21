package io.harness.batch.processing.tasklet;

import static io.harness.batch.processing.ccm.CCMJobConstants.ACCOUNT_ID;
import static io.harness.rule.OwnerRule.HANTANG;
import static io.harness.rule.OwnerRule.ROHIT;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.config.BillingDataPipelineConfig;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.batch.processing.service.intfc.BillingDataPipelineService;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.entities.BillingDataPipelineRecord;
import io.harness.ccm.config.GcpBillingAccount;
import io.harness.ccm.config.GcpOrganization;
import io.harness.ccm.config.GcpOrganizationDao;
import io.harness.rule.Owner;

import software.wings.beans.Account;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;

@RunWith(MockitoJUnitRunner.class)
public class GcpBillingDataPipelineTaskletTest {
  private static final String accountId = "accountId";
  private static final String accountName = "accountName";
  private static final String gcpProjectId = "projectId";
  private static final String dataSetId = "datasetId";
  private static final String bqProjectId = "BQ_PROJECT_ID";
  private static final String bqDatasetId = "BQ_DATASET_ID";
  private static final String bqRegion = "BQ_REGION";
  private ChunkContext chunkContext;

  Instant instant = Instant.now();
  private long startTime = instant.toEpochMilli();
  private long endTime = instant.plus(1, ChronoUnit.DAYS).toEpochMilli();

  @Mock private BatchMainConfig mainConfig;
  @Mock CloudToHarnessMappingService cloudToHarnessMappingService;
  @Mock BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  @Mock BillingDataPipelineService billingDataPipelineService;
  @Mock GcpOrganizationDao gcpOrganizationDao;
  @InjectMocks GcpBillingDataPipelineTasklet gcpBillingDataPipelineTasklet;

  @Before
  public void setup() throws IOException {
    Map<String, JobParameter> parameters = new HashMap<>();
    parameters.put(CCMJobConstants.JOB_START_DATE, new JobParameter(String.valueOf(startTime), true));
    parameters.put(CCMJobConstants.JOB_END_DATE, new JobParameter(String.valueOf(endTime), true));
    parameters.put(ACCOUNT_ID, new JobParameter(ACCOUNT_ID, true));
    JobParameters jobParameters = new JobParameters(parameters);
    StepExecution stepExecution = new StepExecution("gcpBillingDataPipelineStep", new JobExecution(0L, jobParameters));
    chunkContext = new ChunkContext(new StepContext(stepExecution));

    BillingDataPipelineConfig billingDataPipelineConfig =
        BillingDataPipelineConfig.builder().gcpProjectId(gcpProjectId).build();
    when(mainConfig.getBillingDataPipelineConfig()).thenReturn(billingDataPipelineConfig);

    Account account = Account.Builder.anAccount().withUuid(accountId).withAccountName(accountName).build();
    when(cloudToHarnessMappingService.getAccountInfoFromId(accountId)).thenReturn(account);

    List<GcpBillingAccount> gcpBillingAccounts = Arrays.asList(GcpBillingAccount.builder()
                                                                   .accountId(accountId)
                                                                   .bqProjectId(bqProjectId)
                                                                   .bqDatasetId(bqDatasetId)
                                                                   .bqDataSetRegion(bqRegion)
                                                                   .build());
    when(cloudToHarnessMappingService.listGcpBillingAccountUpdatedInDuration(eq(accountId), eq(startTime), eq(endTime)))
        .thenReturn(gcpBillingAccounts);
    when(billingDataPipelineService.createDataSet(eq(account))).thenReturn(dataSetId);

    GcpOrganization gcpOrganization = GcpOrganization.builder().build();
    when(gcpOrganizationDao.get(anyString())).thenReturn(gcpOrganization);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldExecute() throws Exception {
    gcpBillingDataPipelineTasklet.execute(null, chunkContext);
    verify(billingDataPipelineRecordDao)
        .create(eq(BillingDataPipelineRecord.builder()
                       .accountId(accountId)
                       .accountName(accountName)
                       .cloudProvider("GCP")
                       .dataSetId(dataSetId)
                       .dataTransferJobName("BigQueryCopyTransferJob_BQ_PROJECT_ID_BQ_DATASET_ID")
                       .preAggregatedScheduledQueryName("gcpPreAggQuery_BQ_PROJECT_ID_BQ_DATASET_ID")
                       .gcpBqProjectId(bqProjectId)
                       .gcpBqDatasetId(bqDatasetId)
                       .build()));
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldExecuteForUSRegion() throws Exception {
    List<GcpBillingAccount> gcpBillingAccounts = Arrays.asList(GcpBillingAccount.builder()
                                                                   .accountId(accountId)
                                                                   .bqProjectId(bqProjectId)
                                                                   .bqDatasetId(bqDatasetId)
                                                                   .bqDataSetRegion("US")
                                                                   .build());
    when(cloudToHarnessMappingService.listGcpBillingAccountUpdatedInDuration(eq(accountId), eq(startTime), eq(endTime)))
        .thenReturn(gcpBillingAccounts);
    gcpBillingDataPipelineTasklet.execute(null, chunkContext);
    verify(billingDataPipelineRecordDao)
        .create(eq(BillingDataPipelineRecord.builder()
                       .accountId(accountId)
                       .accountName(accountName)
                       .cloudProvider("GCP")
                       .dataSetId(dataSetId)
                       .dataTransferJobName("gcpCopyScheduledQuery_BQ_PROJECT_ID_BQ_DATASET_ID")
                       .preAggregatedScheduledQueryName("gcpPreAggQuery_BQ_PROJECT_ID_BQ_DATASET_ID")
                       .gcpBqProjectId(bqProjectId)
                       .gcpBqDatasetId(bqDatasetId)
                       .build()));
  }
}
