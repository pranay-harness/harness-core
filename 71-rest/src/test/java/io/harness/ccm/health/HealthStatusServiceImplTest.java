package io.harness.ccm.health;

import static io.harness.ccm.health.CEConnectorHealthMessages.BILLING_DATA_PIPELINE_SUCCESS;
import static io.harness.ccm.health.CEConnectorHealthMessages.BILLING_PIPELINE_CREATION_SUCCESSFUL;
import static io.harness.ccm.health.CEConnectorHealthMessages.SETTING_ATTRIBUTE_CREATED;
import static io.harness.rule.OwnerRule.HANTANG;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.when;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.dao.BillingDataPipelineRecordDao;
import io.harness.ccm.cluster.entities.BillingDataPipelineRecord;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.LastReceivedPublishedMessage;
import io.harness.ccm.config.CCMConfig;
import io.harness.ccm.config.CCMSettingService;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskState;
import io.harness.perpetualtask.internal.PerpetualTaskRecord;
import io.harness.rule.Owner;
import io.harness.time.FakeClock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.service.intfc.SettingsService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public class HealthStatusServiceImplTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private static final String masterUrl = "dummyMasterUrl";
  private static final String username = "dummyUsername";
  private static final String password = "dummyPassword";
  private CCMConfig ccmConfig = CCMConfig.builder().cloudCostEnabled(true).build();
  private String cloudProviderId = "CLOUD_PROVIDER_ID";
  private String awsConnectorId = "AWS_CONNECTOR_ID";
  private String clusterId = "CLUSTER_ID";
  private String datasetId = "DATA_SET_ID";
  private static final String status = "SUCCEEDED";
  private static final String s3SyncMessage = "Last Successful S3 Sync at";

  private SettingAttribute cloudProvider;
  private Cluster k8sCluster;
  private ClusterRecord clusterRecord;
  private String[] perpetualTaskIds = new String[] {"1", "2"};
  private String delegateId = "DELEGATE_ID";
  private PerpetualTaskRecord taskRecord;
  private SettingAttribute awsConnector;
  Instant currentInstant;

  FakeClock fakeClock = new FakeClock();

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock SettingsService settingsService;
  @Mock CCMSettingService ccmSettingService;
  @Mock ClusterRecordService clusterRecordService;
  @Mock PerpetualTaskService perpetualTaskService;
  @Mock LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;
  @Mock BillingDataPipelineRecordDao billingDataPipelineRecordDao;
  @Mock CeExceptionRecordDao ceExceptionRecordDao;

  @InjectMocks HealthStatusServiceImpl healthStatusService;

  @Before
  public void setUp() throws Exception {
    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder()
                                                          .masterUrl(masterUrl)
                                                          .username(username)
                                                          .password(password.toCharArray())
                                                          .accountId(accountId)
                                                          .ccmConfig(ccmConfig)
                                                          .build();
    String kubernetesClusterConfigName = "KubernetesCluster-" + System.currentTimeMillis();
    cloudProvider = aSettingAttribute()
                        .withCategory(SettingCategory.CLOUD_PROVIDER)
                        .withUuid(cloudProviderId)
                        .withAccountId(accountId)
                        .withName(kubernetesClusterConfigName)
                        .withValue(kubernetesClusterConfig)
                        .build();

    awsConnector = aSettingAttribute()
                       .withCategory(SettingCategory.CE_CONNECTOR)
                       .withUuid(awsConnectorId)
                       .withAccountId(accountId)
                       .withValue(CEAwsConfig.builder().build())
                       .build();

    k8sCluster = DirectKubernetesCluster.builder().cloudProviderId(cloudProviderId).build();
    clusterRecord = ClusterRecord.builder()
                        .accountId(accountId)
                        .uuid(clusterId)
                        .cluster(k8sCluster)
                        .perpetualTaskIds(perpetualTaskIds)
                        .build();

    taskRecord = getPerpetualTaskRecord(delegateId, PerpetualTaskState.TASK_RUN_SUCCEEDED);
    currentInstant = Instant.now();

    when(billingDataPipelineRecordDao.fetchBillingPipelineRecord(accountId, awsConnectorId))
        .thenReturn(BillingDataPipelineRecord.builder()
                        .dataSetId(datasetId)
                        .lastSuccessfulS3Sync(currentInstant.minus(1, ChronoUnit.DAYS))
                        .dataTransferJobStatus(status)
                        .awsFallbackTableScheduledQueryStatus(status)
                        .preAggregatedScheduledQueryStatus(status)
                        .build());

    when(settingsService.get(eq(cloudProviderId))).thenReturn(cloudProvider);
    when(ccmSettingService.isCloudCostEnabled(isA(SettingAttribute.class))).thenReturn(true);
    when(clusterRecordService.list(eq(accountId), eq(null), eq(cloudProviderId)))
        .thenReturn(Arrays.asList(clusterRecord));
    when(perpetualTaskService.getTaskRecord(anyString())).thenReturn(taskRecord);
    when(lastReceivedPublishedMessageDao.get(eq(accountId), eq(clusterId)))
        .thenReturn(
            LastReceivedPublishedMessage.builder().lastReceivedAt(Instant.now(fakeClock).toEpochMilli()).build());
  }

  private PerpetualTaskRecord getPerpetualTaskRecord(String delegateId, PerpetualTaskState state) {
    return PerpetualTaskRecord.builder().delegateId(delegateId).state(state.name()).build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldThrowExceptionForCEDisabledCloudProviders() {
    when(ccmSettingService.isCloudCostEnabled(isA(SettingAttribute.class))).thenReturn(false);
    assertThatThrownBy(() -> healthStatusService.getHealthStatus(cloudProviderId))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldReturnUnhealthyWhenDelegateDisconnected() {
    PerpetualTaskRecord taskRecord = getPerpetualTaskRecord(null, PerpetualTaskState.NO_DELEGATE_AVAILABLE);
    when(perpetualTaskService.getTaskRecord(anyString())).thenReturn(taskRecord);
    CEHealthStatus status = healthStatusService.getHealthStatus(cloudProviderId);
    assertThat(status.isHealthy()).isFalse();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldReturnHealthyForCloudProviders() {
    when(ccmSettingService.isCloudCostEnabled(isA(SettingAttribute.class))).thenReturn(true);
    CEHealthStatus status = healthStatusService.getHealthStatus(cloudProviderId);
    assertThat(status.isHealthy()).isTrue();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void healthStatusForAConnectorJustCreated() {
    when(settingsService.get(awsConnectorId)).thenReturn(awsConnector);
    awsConnector.setCreatedAt(currentInstant.minus(0, ChronoUnit.DAYS).toEpochMilli());
    CEHealthStatus healthStatus = healthStatusService.getHealthStatus(awsConnectorId);
    assertThat(healthStatus.getClusterHealthStatusList().get(0).getMessages().get(0))
        .isEqualTo(SETTING_ATTRIBUTE_CREATED.getMessage());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void healthStatusForCaseOfSuccessfulPipelineCreation() {
    when(settingsService.get(awsConnectorId)).thenReturn(awsConnector);
    awsConnector.setCreatedAt(currentInstant.minus(1, ChronoUnit.DAYS).toEpochMilli());
    CEHealthStatus healthStatus = healthStatusService.getHealthStatus(awsConnectorId);
    assertThat(healthStatus.getClusterHealthStatusList().get(0).getMessages().get(0))
        .isEqualTo(BILLING_PIPELINE_CREATION_SUCCESSFUL.getMessage());
    assertThat(healthStatus.getClusterHealthStatusList().get(0).getMessages().get(1)).contains(s3SyncMessage);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void healthStatusForAConnectorRunningSuccessfullyForFiveDays() {
    when(settingsService.get(awsConnectorId)).thenReturn(awsConnector);
    awsConnector.setCreatedAt(currentInstant.minus(5, ChronoUnit.DAYS).toEpochMilli());
    CEHealthStatus healthStatus = healthStatusService.getHealthStatus(awsConnectorId);
    assertThat(healthStatus.getClusterHealthStatusList().get(0).getMessages().get(0))
        .isEqualTo(BILLING_DATA_PIPELINE_SUCCESS.getMessage());
    assertThat(healthStatus.getClusterHealthStatusList().get(0).getMessages().get(1)).contains(s3SyncMessage);
  }
}