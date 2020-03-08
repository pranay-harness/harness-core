package io.harness.ccm.cluster;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.dao.ClusterRecordDao;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.List;

public class ClusterRecordDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";

  private String k8sCloudProviderId = "K8S_CLOUD_PROVIDER_ID";
  private DirectKubernetesCluster k8sCluster;
  private ClusterRecord k8sClusterRecord;

  private String ecsCloudProviderId = "ECS_CLOUD_PROVIDER_ID";
  private String region = "REGION";

  private String clusterName = "CLUSTER_NAME";
  private EcsCluster ecsCluster;
  private ClusterRecord ecsClusterRecord;

  private String clusterName2 = "CLUSTER_NAME_2";
  private EcsCluster ecsCluster2;
  private ClusterRecord ecsClusterRecord2;

  @Inject private ClusterRecordDao clusterRecordDao;

  @Before
  public void setUp() {
    k8sCluster = DirectKubernetesCluster.builder().cloudProviderId(k8sCloudProviderId).build();
    k8sClusterRecord = ClusterRecord.builder().accountId(accountId).cluster(k8sCluster).build();

    ecsCluster =
        EcsCluster.builder().cloudProviderId(ecsCloudProviderId).region(region).clusterName(clusterName).build();
    ecsClusterRecord = ClusterRecord.builder().accountId(accountId).cluster(ecsCluster).build();
    ecsCluster2 =
        EcsCluster.builder().cloudProviderId(ecsCloudProviderId).region(region).clusterName(clusterName2).build();
    ecsClusterRecord2 = ClusterRecord.builder().accountId(accountId).cluster(ecsCluster2).build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldUpsertK8sCluster() {
    ClusterRecord actualClusterRecord1 = clusterRecordDao.upsertCluster(k8sClusterRecord);
    assertThat(actualClusterRecord1.getAccountId()).isEqualTo(k8sClusterRecord.getAccountId());
    assertThat(actualClusterRecord1.getCluster()).isEqualTo(k8sClusterRecord.getCluster());

    // should not create a second record
    ClusterRecord actualClusterRecord2 = clusterRecordDao.upsertCluster(k8sClusterRecord);
    assertThat(actualClusterRecord2).isEqualToIgnoringGivenFields(actualClusterRecord1, "lastUpdatedAt");
    assertThat(actualClusterRecord2.getLastUpdatedAt()).isNotEqualTo(actualClusterRecord1.getLastUpdatedAt());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldUpsertEcsCluster() {
    ClusterRecord actualClusterRecord = clusterRecordDao.upsertCluster(ecsClusterRecord);
    assertThat(actualClusterRecord.getAccountId()).isEqualTo(ecsClusterRecord.getAccountId());
    assertThat(actualClusterRecord.getCluster()).isEqualTo(ecsClusterRecord.getCluster());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGetClusterByEntity() {
    ClusterRecord upsertedCluster1 = clusterRecordDao.upsertCluster(k8sClusterRecord);
    ClusterRecord actualClusterRecord1 = clusterRecordDao.get(k8sClusterRecord);
    assertThat(actualClusterRecord1).isEqualTo(upsertedCluster1);

    ClusterRecord upsertedCluster2 = clusterRecordDao.upsertCluster(ecsClusterRecord);
    ClusterRecord actualClusterRecord2 = clusterRecordDao.get(ecsClusterRecord);
    assertThat(actualClusterRecord2).isEqualTo(upsertedCluster2);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGetClusterById() {
    ClusterRecord upsertedCluster = clusterRecordDao.upsertCluster(k8sClusterRecord);
    ClusterRecord actualClusterRecord = clusterRecordDao.get(upsertedCluster.getUuid());
    assertThat(actualClusterRecord.getUuid()).isEqualTo(upsertedCluster.getUuid());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldListAllClusters() {
    clusterRecordDao.upsertCluster(ecsClusterRecord);
    clusterRecordDao.upsertCluster(ecsClusterRecord2);
    List<ClusterRecord> clusterRecordList = clusterRecordDao.list(accountId, ecsCloudProviderId, 0, 0);
    assertThat(clusterRecordList).hasSize(2);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldListPaginatedClusters() {
    clusterRecordDao.upsertCluster(ecsClusterRecord);
    clusterRecordDao.upsertCluster(ecsClusterRecord2);
    List<ClusterRecord> clusterRecordList = clusterRecordDao.list(accountId, ecsCloudProviderId, 1, 0);
    assertThat(clusterRecordList).hasSize(1);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldDeleteExistingCluster() {
    ClusterRecord upsertedCluster = clusterRecordDao.upsertCluster(k8sClusterRecord);
    Boolean pass = clusterRecordDao.delete(upsertedCluster);
    assertThat(pass).isTrue();
    List<ClusterRecord> clusterRecordList = clusterRecordDao.list(accountId, k8sCloudProviderId, 0, 0);
    assertThat(clusterRecordList).isNullOrEmpty();
  }

  @Test
  @Owner(developers = HANTANG)
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
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldFailToDeleteNonExistingCluster() {
    Boolean pass1 = clusterRecordDao.delete(k8sClusterRecord);
    assertThat(pass1).isFalse();
  }
}
