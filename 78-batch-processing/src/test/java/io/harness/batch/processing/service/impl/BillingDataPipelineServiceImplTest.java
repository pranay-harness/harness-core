package io.harness.batch.processing.service.impl;

import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.preAggQueryKey;
import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.scheduledQueryKey;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;

import io.harness.batch.processing.config.AwsDataPipelineConfig;
import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.util.HashMap;

@RunWith(MockitoJUnitRunner.class)
public class BillingDataPipelineServiceImplTest {
  @InjectMocks @Spy BillingDataPipelineServiceImpl billingDataPipelineService;
  @Mock BatchMainConfig configuration;

  private static final String settingId = "settingId";
  private static final String accountId = "accountId";
  private static final String accountName = "accountName";
  private static final String dataSetId = "datasetId";
  private static final String transferJobName = "gcsToBigQueryTransferJob_accountname_accountid";
  private static final String scheduledQueryName = "scheduledQuery_accountname_accountid";
  private static final String preAggQueryName = "preAggQuery_accountname_accountid";

  private static final String gcpServiceAccount = "serviceAccount";
  private static final String gcpProjectId = "projectId";
  private static final String gcsBasePath = "gs://bucketName";

  private static ServiceAccountCredentials mockCredential;
  private static DataTransferServiceClient dataTransferServiceClient;

  @Before
  public void setup() throws IOException {
    AwsDataPipelineConfig awsDataPipelineConfig = AwsDataPipelineConfig.builder()
                                                      .gcpProjectId(gcpProjectId)
                                                      .gcpServiceAccount(gcpServiceAccount)
                                                      .gcsBasePath(gcsBasePath)
                                                      .build();
    when(configuration.getAwsDataPipelineConfig()).thenReturn(awsDataPipelineConfig);

    mockCredential = mock(ServiceAccountCredentials.class);
    doReturn(mockCredential).when(billingDataPipelineService).getCredentials();

    dataTransferServiceClient = mock(DataTransferServiceClient.class);
    doReturn(dataTransferServiceClient).when(billingDataPipelineService).getDataTransferClient();
    doNothing().when(billingDataPipelineService).executeDataTransferJobCreate(any(), any());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void createScheduledQueries() throws IOException {
    HashMap<String, String> scheduledQueriesMap =
        billingDataPipelineService.createScheduledQueries(dataSetId, accountId, accountName);
    assertThat(scheduledQueriesMap.get(scheduledQueryKey)).isEqualTo(scheduledQueryName);
    assertThat(scheduledQueriesMap.get(preAggQueryKey)).isEqualTo(preAggQueryName);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void createDataTransferJob() throws IOException {
    String dataTransferJob =
        billingDataPipelineService.createDataTransferJob(dataSetId, settingId, accountId, accountName);
    assertThat(dataTransferJob).isEqualTo(transferJobName);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void createDataSet() {
    String dataSetName = billingDataPipelineService.createDataSet(accountId, accountName);
    assertThat(dataSetName).isNull();
  }
}