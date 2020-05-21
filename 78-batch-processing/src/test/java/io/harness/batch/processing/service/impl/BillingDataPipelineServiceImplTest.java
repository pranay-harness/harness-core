package io.harness.batch.processing.service.impl;

import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.preAggQueryKey;
import static io.harness.batch.processing.service.impl.BillingDataPipelineServiceImpl.scheduledQueryKey;
import static io.harness.rule.OwnerRule.HANTANG;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.datatransfer.v1.DataTransferServiceClient;

import io.harness.batch.processing.config.BatchMainConfig;
import io.harness.batch.processing.config.BillingDataPipelineConfig;
import io.harness.batch.processing.dao.intfc.BillingDataPipelineRecordDao;
import io.harness.category.element.UnitTests;
import io.harness.ccm.billing.GcpServiceAccountServiceImpl;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.HashMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest(GcpServiceAccountServiceImpl.class)
public class BillingDataPipelineServiceImplTest {
  private static final String settingId = "settingId";
  private static final String accountId = "accountId";
  private static final String accountName = "accountName";
  private static final String dataSetId = "datasetId";
  private static final String transferJobName = "gcsToBigQueryTransferJob_accountname_accountid";
  private static final String scheduledQueryName = "scheduledQuery_accountname_accountid";
  private static final String preAggQueryName = "awsPreAggQuery_accountname_accountid";

  private static final String gcpServiceAccount = "serviceAccount";
  private static final String gcpProjectId = "projectId";
  private static final String gcsBasePath = "gs://bucketName";

  @Mock private static ServiceAccountCredentials mockCredential;
  @Mock private static DataTransferServiceClient dataTransferServiceClient;
  @Mock private BatchMainConfig mainConfig;
  @Mock private BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  @InjectMocks
  @Spy
  private BillingDataPipelineServiceImpl billingDataPipelineService =
      new BillingDataPipelineServiceImpl(mainConfig, billingDataPipelineRecordDao);

  @Before
  public void setup() throws IOException {
    BillingDataPipelineConfig billingDataPipelineConfig = BillingDataPipelineConfig.builder()
                                                              .gcpProjectId(gcpProjectId)
                                                              .gcpServiceAccount(gcpServiceAccount)
                                                              .gcsBasePath(gcsBasePath)
                                                              .build();
    when(mainConfig.getBillingDataPipelineConfig()).thenReturn(billingDataPipelineConfig);
    when(billingDataPipelineRecordDao.getByAccountId(accountId)).thenReturn(null);
    mockCredential = mock(ServiceAccountCredentials.class);
    PowerMockito.mockStatic(GcpServiceAccountServiceImpl.class);
    BDDMockito.given(GcpServiceAccountServiceImpl.getCredentials(anyString())).willReturn(mockCredential);
    BDDMockito.given(GcpServiceAccountServiceImpl.getImpersonatedCredentials(any(), any())).willReturn(mockCredential);

    dataTransferServiceClient = mock(DataTransferServiceClient.class);
    doReturn(dataTransferServiceClient).when(billingDataPipelineService).getDataTransferClient();
    doReturn(dataTransferServiceClient).when(billingDataPipelineService).getDataTransferClient(isA(Credentials.class));
    doNothing().when(billingDataPipelineService).executeDataTransferJobCreate(any(), any());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void createScheduledQueries() throws IOException {
    HashMap<String, String> scheduledQueriesMap =
        billingDataPipelineService.createScheduledQueriesForAWS(dataSetId, accountId, accountName);
    assertThat(scheduledQueriesMap.get(scheduledQueryKey)).isEqualTo(scheduledQueryName);
    assertThat(scheduledQueriesMap.get(preAggQueryKey)).isEqualTo(preAggQueryName);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldCopy() throws IOException {
    String jobName = "copy-java-api-display-name";
    String srcProjectId = "tough-talent-271423";
    String srcDatasetId = "billing";
    String dstProjectId = "ccm-play";
    String dstDatasetId = "copy_dataset_cli";
    billingDataPipelineService.createDataTransferJobFromBQ(
        jobName, srcProjectId, srcDatasetId, dstProjectId, dstDatasetId, null);
    verify(billingDataPipelineService, times(1)).executeDataTransferJobCreate(any(), any());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldCreateDataTransferJob() throws IOException {
    String dataTransferJob =
        billingDataPipelineService.createDataTransferJobFromGCS(dataSetId, settingId, accountId, accountName);
    assertThat(dataTransferJob).isEqualTo(transferJobName);
  }
}