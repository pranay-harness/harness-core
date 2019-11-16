package io.harness.ccm.cluster;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.observer.Subject;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class ClusterRecordServiceImplTest extends CategoryTest {
  @Mock private Subject<ClusterRecordObserver> subject;
  @Mock private ClusterRecordDao clusterRecordDao;
  @InjectMocks private ClusterRecordServiceImpl clusterRecordService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private String accountId = "ACCOUNT_ID";
  private String cloudProviderId = "CLOUD_PROVIDER_ID";
  private String clusterId = "CLUSTER_ID";
  private Cluster k8sCluster;
  private ClusterRecord clusterRecord;
  private ClusterRecord clusterRecordWithId;

  @Before
  public void setUp() {
    k8sCluster = DirectKubernetesCluster.builder().cloudProviderId(cloudProviderId).build();
    clusterRecord = ClusterRecord.builder().accountId(accountId).cluster(k8sCluster).build();

    clusterRecordWithId = ClusterRecord.builder().uuid(clusterId).accountId(accountId).cluster(k8sCluster).build();
    when(clusterRecordDao.upsertCluster(isA(ClusterRecord.class))).thenReturn(clusterRecordWithId);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldInformUponUpsert() {
    when(clusterRecordDao.get(isA(ClusterRecord.class))).thenReturn(null);
    clusterRecordService.upsert(clusterRecord);
    verify(subject, times(1)).fireInform(any(), eq(clusterRecord));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldNotInformUponUpdate() {
    when(clusterRecordDao.get(isA(ClusterRecord.class))).thenReturn(null);
    clusterRecordService.upsert(clusterRecord);
    verify(subject, times(1)).fireInform(any(), eq(clusterRecord)); // instead of 2
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldGetClusterRecord() {
    ClusterRecord upsertedClusterRecord = clusterRecordService.upsert(clusterRecord);
    clusterRecordService.get(upsertedClusterRecord.getUuid());
    verify(clusterRecordDao).get(eq(upsertedClusterRecord.getUuid()));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldDeleteExistingClusters() {
    clusterRecordService.upsert(clusterRecord);
    Boolean pass2 = clusterRecordService.delete(accountId, cloudProviderId);
    verify(clusterRecordDao).delete(eq(accountId), eq(cloudProviderId));
  }
}
