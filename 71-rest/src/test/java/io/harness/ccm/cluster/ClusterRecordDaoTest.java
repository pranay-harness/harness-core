package io.harness.ccm.cluster;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ClusterRecordDaoTest extends WingsBaseTest {
  @Inject @InjectMocks private ClusterRecordDao clusterRecordDao;

  String accountId = "ACCOUNT_ID";
  String k8sCloudProviderId = "K8S_CLOUD_PROVIDER_ID";
  DirectKubernetesCluster k8sCluster;
  ClusterRecord k8sClusterRecord;

  String ecsCloudProviderId = "ECS_CLOUD_PROVIDER_ID";
  String region = "REGION";
  String clusterName = "CLUSTER_NAME";
  EcsCluster ecsCluster;
  ClusterRecord ecsClusterRecord;

  @Before
  public void setUp() {
    k8sCluster = DirectKubernetesCluster.builder().cloudProviderId(k8sCloudProviderId).build();
    k8sClusterRecord = ClusterRecord.builder().accountId(accountId).cluster(k8sCluster).build();

    ecsCluster =
        EcsCluster.builder().cloudProviderId(ecsCloudProviderId).region(region).clusterName(clusterName).build();
    ecsClusterRecord = ClusterRecord.builder().accountId(accountId).cluster(ecsCluster).build();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldUpsertK8sCluster() {
    ClusterRecord actualClusterRecord1 = clusterRecordDao.upsertCluster(k8sClusterRecord);
    assertThat(actualClusterRecord1.getAccountId()).isEqualTo(k8sClusterRecord.getAccountId());
    assertThat(actualClusterRecord1.getCluster()).isEqualTo(k8sClusterRecord.getCluster());

    // should not create a second record
    ClusterRecord actualClusterRecord2 = clusterRecordDao.upsertCluster(k8sClusterRecord);
    assertThat(actualClusterRecord2).isEqualTo(actualClusterRecord1);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldGetCluster() {
    ClusterRecord actualClusterRecord1 = clusterRecordDao.get(k8sClusterRecord);
    assertThat(actualClusterRecord1).isNull();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldListClusters() {
    ClusterRecord upsertedClusterRecord1 = clusterRecordDao.upsertCluster(k8sClusterRecord);
    List<ClusterRecord> clusterRecordList = clusterRecordDao.list(accountId, k8sCloudProviderId);
    assertThat(clusterRecordList).isEqualTo(new ArrayList<>(Arrays.asList(upsertedClusterRecord1)));
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldDeleteExistingCluster() {
    ClusterRecord upsertedCluster = clusterRecordDao.upsertCluster(k8sClusterRecord);
    Boolean pass = clusterRecordDao.delete(upsertedCluster);
    assertThat(pass).isTrue();
    List<ClusterRecord> clusterRecordList = clusterRecordDao.list(accountId, k8sCloudProviderId);
    assertThat(clusterRecordList).isNullOrEmpty();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldUpsertEcsCluster() {
    ClusterRecord actualClusterRecord = clusterRecordDao.upsertCluster(ecsClusterRecord);
    assertThat(actualClusterRecord.getAccountId()).isEqualTo(ecsClusterRecord.getAccountId());
    assertThat(actualClusterRecord.getCluster()).isEqualTo(ecsClusterRecord.getCluster());
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldInsertAndRemoveTask() {
    String taskId = "TASK_ID";
    ClusterRecord clusterRecordNoTask = clusterRecordDao.upsertCluster(ecsClusterRecord);
    ClusterRecord clusterRecordWithTask = clusterRecordDao.insertTask(clusterRecordNoTask, taskId);
    assertThat(clusterRecordWithTask.getPerpetualTaskIds()).contains(taskId);

    ClusterRecord actualClusterRecord = clusterRecordDao.removeTask(clusterRecordWithTask, taskId);
    assertThat(actualClusterRecord.getPerpetualTaskIds()).doesNotContain(taskId);
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void shouldFailToDeleteNonExistingCluster() {
    Boolean pass1 = clusterRecordDao.delete(k8sClusterRecord);
    assertThat(pass1).isFalse();
  }
}
