package io.harness.ccm;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.perpetualtask.PerpetualTaskType.K8S_WATCH;
import static io.harness.rule.OwnerRule.HANTANG;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.perpetualtask.PerpetualTaskType;
import io.harness.perpetualtask.ecs.EcsPerpetualTaskServiceClient;
import io.harness.perpetualtask.k8s.watch.K8sWatchPerpetualTaskServiceClient;
import io.harness.rule.Owner;
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

import java.util.ArrayList;
import java.util.Arrays;

public class CCMPerpetualTaskManagerTest extends CategoryTest {
  private String clusterRecordId = "clusterId";
  private String clusterName = "clusterName";
  private String accountId = "ACCOUNT_ID";
  private String region = "region";
  private String cloudProviderId = "CLOUD_PROVIDER_ID";

  private static final String masterUrl = "dummyMasterUrl";
  public static final String username = "dummyUsername";
  public static final String password = "dummyPassword";
  private SettingAttribute cloudProvider;

  private Cluster k8sCluster;
  private Cluster ecsCluster;
  private ClusterRecord clusterRecord;
  private ClusterRecord ecsClusterRecord;

  private String podTaskId = "POD_WATCHER_TASK_ID";
  private String nodeTaskId = "NODE_WATCHER_TASK_ID";
  PageResponse<ClusterRecord> response;

  @Mock private ClusterRecordService clusterRecordService;
  @Mock private PerpetualTaskService perpetualTaskService;
  @Mock private PerpetualTaskServiceClientRegistry clientRegistry;
  @Mock private K8sWatchPerpetualTaskServiceClient k8sWatchPerpetualTaskServiceClient;
  @Mock private EcsPerpetualTaskServiceClient ecsPerpetualTaskServiceClient;

  @Inject @InjectMocks CCMPerpetualTaskManager manager;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setUp() {
    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder().accountId(accountId).build();
    cloudProvider = aSettingAttribute()
                        .withCategory(SettingCategory.CLOUD_PROVIDER)
                        .withUuid(cloudProviderId)
                        .withAccountId(accountId)
                        .withValue(kubernetesClusterConfig)
                        .build();

    k8sCluster = DirectKubernetesCluster.builder().clusterName(clusterName).cloudProviderId(cloudProviderId).build();
    ecsCluster = EcsCluster.builder().clusterName(clusterName).cloudProviderId(cloudProviderId).region(region).build();
    clusterRecord = ClusterRecord.builder().uuid(clusterRecordId).accountId(accountId).cluster(k8sCluster).build();
    ecsClusterRecord = ClusterRecord.builder().uuid(clusterRecordId).accountId(accountId).cluster(ecsCluster).build();

    response = aPageResponse().withResponse(Arrays.asList(clusterRecord)).build();
    when(clusterRecordService.list(eq(accountId), eq(cloudProviderId))).thenReturn(response);
    when(clusterRecordService.attachPerpetualTaskId(eq(clusterRecord), anyString())).thenReturn(clusterRecord);
    when(clusterRecordService.removePerpetualTaskId(isA(ClusterRecord.class), anyString())).thenReturn(clusterRecord);
    when(clientRegistry.getClient(eq(PerpetualTaskType.K8S_WATCH))).thenReturn(k8sWatchPerpetualTaskServiceClient);
    when(clientRegistry.getClient(eq(PerpetualTaskType.ECS_CLUSTER))).thenReturn(ecsPerpetualTaskServiceClient);
    when(perpetualTaskService.getPerpetualTaskType(anyString())).thenReturn(K8S_WATCH);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldCreatePerpetualTasksForCloudProvider() {
    manager.createPerpetualTasks(cloudProvider);
    verify(clusterRecordService, times(1)).attachPerpetualTaskId(eq(clusterRecord), anyString());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldResetPerpetualTasksForCloudProvider() {
    manager.createPerpetualTasks(cloudProvider);
    String[] tasks = {podTaskId, nodeTaskId};
    clusterRecord.setPerpetualTaskIds(tasks);
    manager.resetPerpetualTasks(clusterRecord);
    verify(perpetualTaskService, times(2)).getPerpetualTaskType(anyString());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldDeletePerpetualTasksForCloudProvider() {
    String[] tasks = {podTaskId, nodeTaskId};
    clusterRecord.setPerpetualTaskIds(tasks);
    when(clusterRecordService.list(eq(accountId), eq(cloudProviderId)))
        .thenReturn(new ArrayList<>(Arrays.asList(clusterRecord)));
    manager.deletePerpetualTasks(cloudProvider);
    verify(clusterRecordService, times(2)).removePerpetualTaskId(isA(ClusterRecord.class), anyString());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldCreatePerpetualTasksForCluster() {
    manager.createPerpetualTasks(clusterRecord);
    verify(clusterRecordService, times(1)).attachPerpetualTaskId(eq(clusterRecord), anyString());
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldCreatePerpetualTasksForECSCluster() {
    manager.createPerpetualTasks(ecsClusterRecord);
    verify(clusterRecordService, times(1)).attachPerpetualTaskId(eq(ecsClusterRecord), anyString());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldResetPerpetualTasksForCluster() {
    manager.createPerpetualTasks(clusterRecord);
    String[] tasks = {podTaskId, nodeTaskId};
    clusterRecord.setPerpetualTaskIds(tasks);
    manager.resetPerpetualTasks(clusterRecord);
    verify(perpetualTaskService, times(2)).getPerpetualTaskType(anyString());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldDeletePerpetualTaskForCluster() {
    manager.createPerpetualTasks(clusterRecord);
    manager.deletePerpetualTasks(clusterRecord);
    verify(clusterRecordService, never()).removePerpetualTaskId(isA(ClusterRecord.class), anyString());

    manager.createPerpetualTasks(clusterRecord);
    String[] tasks = {podTaskId, nodeTaskId};
    clusterRecord.setPerpetualTaskIds(tasks);
    manager.deletePerpetualTasks(clusterRecord);
    verify(clusterRecordService, times(2)).removePerpetualTaskId(isA(ClusterRecord.class), anyString());
  }
}
