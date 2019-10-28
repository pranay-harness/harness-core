package io.harness.ccm.cluster;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.cluster.entities.EcsCluster;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;

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
  @Category(UnitTests.class)
  public void shouldUpsertK8sCluster() {
    ClusterRecord actualClusterRecord1 = clusterRecordDao.upsert(k8sClusterRecord);
    assertThat(actualClusterRecord1.getAccountId()).isEqualTo(k8sClusterRecord.getAccountId());
    assertThat(actualClusterRecord1.getCluster()).isEqualTo(k8sClusterRecord.getCluster());

    // should not create a second record
    ClusterRecord actualClusterRecord2 = clusterRecordDao.upsert(k8sClusterRecord);
    assertThat(actualClusterRecord2).isEqualTo(actualClusterRecord1);
  }

  @Test
  @Category(UnitTests.class)
  public void testUpsertEcsCluster() {
    ClusterRecord actualClusterRecord = clusterRecordDao.upsert(ecsClusterRecord);
    assertThat(actualClusterRecord.getAccountId()).isEqualTo(ecsClusterRecord.getAccountId());
    assertThat(actualClusterRecord.getCluster()).isEqualTo(ecsClusterRecord.getCluster());
  }

  @Test
  @Category(UnitTests.class)
  public void testDelete() {
    Boolean pass1 = clusterRecordDao.delete(accountId, k8sCloudProviderId);
    assertThat(pass1).isFalse();

    clusterRecordDao.upsert(k8sClusterRecord);
    Boolean pass2 = clusterRecordDao.delete(accountId, k8sCloudProviderId);
    assertThat(pass2).isTrue();
  }
}
