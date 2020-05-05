package io.harness.ccm.health;

import static io.harness.rule.OwnerRule.HANTANG;
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
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;

import java.time.Instant;
import java.util.Arrays;

public class HealthStatusServiceImplTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private static final String masterUrl = "dummyMasterUrl";
  private static final String username = "dummyUsername";
  private static final String password = "dummyPassword";
  private CCMConfig ccmConfig = CCMConfig.builder().cloudCostEnabled(true).build();
  private String cloudProviderId = "CLOUD_PROVIDER_ID";
  private String clusterId = "CLUSTER_ID";

  private SettingAttribute cloudProvider;
  private Cluster k8sCluster;
  private ClusterRecord clusterRecord;
  private String[] perpetualTaskIds = new String[] {"1", "2"};
  private String delegateId = "DELEGATE_ID";
  private PerpetualTaskRecord taskRecord;

  FakeClock fakeClock = new FakeClock();

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock SettingsService settingsService;
  @Mock CCMSettingService ccmSettingService;
  @Mock ClusterRecordService clusterRecordService;
  @Mock PerpetualTaskService perpetualTaskService;
  @Mock DelegateService delegateService;
  @Mock LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;
  @Mock CeExceptionRecordDao CeExceptionRecordDao;

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

    k8sCluster = DirectKubernetesCluster.builder().cloudProviderId(cloudProviderId).build();
    clusterRecord = ClusterRecord.builder()
                        .accountId(accountId)
                        .uuid(clusterId)
                        .cluster(k8sCluster)
                        .perpetualTaskIds(perpetualTaskIds)
                        .build();

    taskRecord = PerpetualTaskRecord.builder()
                     .delegateId(delegateId)
                     .state(PerpetualTaskState.TASK_RUN_SUCCEEDED.name())
                     .build();

    when(settingsService.get(eq(cloudProviderId))).thenReturn(cloudProvider);
    when(ccmSettingService.isCloudCostEnabled(isA(SettingAttribute.class))).thenReturn(true);
    when(clusterRecordService.list(eq(accountId), eq(null), eq(cloudProviderId)))
        .thenReturn(Arrays.asList(clusterRecord));
    when(perpetualTaskService.getTaskRecord(anyString())).thenReturn(taskRecord);
    when(delegateService.checkDelegateConnected(eq(delegateId))).thenReturn(true);
    when(lastReceivedPublishedMessageDao.get(eq(accountId), eq(clusterId)))
        .thenReturn(
            LastReceivedPublishedMessage.builder().lastReceivedAt(Instant.now(fakeClock).toEpochMilli()).build());
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
    when(delegateService.checkDelegateConnected(eq(delegateId))).thenReturn(false);
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
}