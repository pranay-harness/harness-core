package io.harness.ccm.cluster;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static software.wings.beans.InfrastructureType.DIRECT_KUBERNETES;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.observer.Subject;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.Application;
import software.wings.beans.DirectKubernetesInfrastructureMapping;
import software.wings.infra.DirectKubernetesInfrastructure;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.AppService;

public class ClusterRecordServiceImplTest extends CategoryTest {
  @Mock private Subject<ClusterRecordObserver> subject;
  @Mock private ClusterRecordDao clusterRecordDao;
  @Mock private AppService appService;
  @InjectMocks private ClusterRecordServiceImpl clusterRecordService;
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  private String accountId = "ACCOUNT_ID";
  private String cloudProviderId = "CLOUD_PROVIDER_ID";
  private String appId = "APP_ID";
  private String clusterId = "CLUSTER_ID";
  private String clusterName = "CLUSTER_NAME";
  private String computeProviderName = "clusterName";
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
    verify(subject, times(1)).fireInform(any(), eq(clusterRecordWithId));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldNotInformUponUpdate() {
    when(clusterRecordDao.get(isA(ClusterRecord.class))).thenReturn(null);
    clusterRecordService.upsert(clusterRecord);
    verify(subject, times(1)).fireInform(any(), eq(clusterRecordWithId)); // instead of 2
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

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testFromInfrastructureDefinition() {
    InfrastructureDefinition infrastructureDefinition = InfrastructureDefinition.builder()
                                                            .appId(appId)
                                                            .infrastructure(DirectKubernetesInfrastructure.builder()
                                                                                .cloudProviderId(cloudProviderId)
                                                                                .clusterName(clusterName)
                                                                                .build())
                                                            .build();

    ClusterRecord expectedClusterRecord =
        ClusterRecord.builder()
            .accountId(accountId)
            .cluster(
                DirectKubernetesCluster.builder().cloudProviderId(cloudProviderId).clusterName(clusterName).build())
            .build();

    Application application = Application.Builder.anApplication().accountId(accountId).build();
    when(appService.get(eq(appId))).thenReturn(application);
    ClusterRecord actualClusterRecord = clusterRecordService.from(infrastructureDefinition);
    assertThat(actualClusterRecord).isEqualTo(expectedClusterRecord);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testFromInfrastructureMapping() {
    DirectKubernetesInfrastructureMapping k8sInfraMapping = DirectKubernetesInfrastructureMapping.builder()
                                                                .accountId(accountId)
                                                                .infraMappingType(DIRECT_KUBERNETES)
                                                                .cloudProviderId(cloudProviderId)
                                                                .build();
    k8sInfraMapping.setComputeProviderName(computeProviderName);

    ClusterRecord expectedClusterRecord = ClusterRecord.builder()
                                              .accountId(accountId)
                                              .cluster(DirectKubernetesCluster.builder()
                                                           .cloudProviderId(cloudProviderId)
                                                           .clusterName(computeProviderName)
                                                           .build())
                                              .build();
    ClusterRecord actualClusterRecord = clusterRecordService.from(k8sInfraMapping);
    assertThat(actualClusterRecord).isEqualTo(expectedClusterRecord);
  }
}
