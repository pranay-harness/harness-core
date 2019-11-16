package io.harness.ccm;

import static io.harness.perpetualtask.PerpetualTaskType.K8S_WATCH;
import static io.harness.rule.OwnerRule.HANTANG;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.perpetualtask.PerpetualTaskService;
import io.harness.perpetualtask.PerpetualTaskServiceClientRegistry;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingCategory;

import java.util.Arrays;

public class CCMPerpetualTaskManagerTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private String cloudProviderId = "CLOUD_PROVIDER_ID";

  private static final String masterUrl = "dummyMasterUrl";
  public static final String username = "dummyUsername";
  public static final String password = "dummyPassword";
  private SettingAttribute cloudProvider;

  private Cluster k8sCluster;
  private ClusterRecord clusterRecord;

  private String podTaskId = "POD_WATCHER_TASK_ID";
  private String nodeTaskId = "NODE_WATCHER_TASK_ID";

  @Mock private ClusterRecordService clusterRecordService;
  @Mock private PerpetualTaskService perpetualTaskService;
  @Mock private PerpetualTaskServiceClientRegistry clientRegistry;
  @Inject @InjectMocks CCMPerpetualTaskManager manager;

  @Before
  public void setUp() {
    KubernetesClusterConfig kubernetesClusterConfig = KubernetesClusterConfig.builder().accountId(accountId).build();
    cloudProvider = aSettingAttribute()
                        .withCategory(SettingCategory.CLOUD_PROVIDER)
                        .withUuid(cloudProviderId)
                        .withAccountId(accountId)
                        .withValue(kubernetesClusterConfig)
                        .build();

    k8sCluster = DirectKubernetesCluster.builder().cloudProviderId(cloudProviderId).build();
    clusterRecord = ClusterRecord.builder().accountId(accountId).cluster(k8sCluster).build();

    when(clusterRecordService.list(eq(accountId), eq(cloudProviderId))).thenReturn(Arrays.asList(clusterRecord));
    when(clusterRecordService.attachPerpetualTaskId(eq(clusterRecord), anyString())).thenReturn(clusterRecord);
    when(clusterRecordService.removePerpetualTaskId(isA(ClusterRecord.class), anyString())).thenReturn(clusterRecord);
    when(perpetualTaskService.getPerpetualTaskType(anyString())).thenReturn(K8S_WATCH);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldCreatePerpetualTasksForCloudProvider() {
    manager.createPerpetualTasks(cloudProvider);
    verify(clusterRecordService, times(2)).attachPerpetualTaskId(eq(clusterRecord), anyString());
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
    manager.createPerpetualTasks(clusterRecord);
    String[] tasks = {podTaskId, nodeTaskId};
    clusterRecord.setPerpetualTaskIds(tasks);
    manager.deletePerpetualTasks(cloudProvider);
    verify(clusterRecordService, times(2)).removePerpetualTaskId(isA(ClusterRecord.class), anyString());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldCreatePerpetualTasksForCluster() {
    manager.createPerpetualTasks(clusterRecord);
    verify(clusterRecordService, times(2)).attachPerpetualTaskId(eq(clusterRecord), anyString());
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
