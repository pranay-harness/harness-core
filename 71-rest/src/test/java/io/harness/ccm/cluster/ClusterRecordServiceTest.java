package io.harness.ccm.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.observer.Subject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

public class ClusterRecordServiceTest extends WingsBaseTest {
  @Mock private Subject<ClusterRecordObserver> subject;
  @Inject @InjectMocks private ClusterRecordService clusterRecordService;

  private String accountId = "ACCOUNT_ID";
  private String cloudProviderId = "CLOUD_PROVIDER_ID";
  private Cluster k8sCluster;
  private ClusterRecord clusterRecord;

  @Before
  public void setUp() {
    k8sCluster = DirectKubernetesCluster.builder().cloudProviderId(cloudProviderId).build();
    clusterRecord = ClusterRecord.builder().accountId(accountId).cluster(k8sCluster).build();
  }

  @Test
  @Category(UnitTests.class)
  public void shouldInformUponUpsert() {
    clusterRecordService.upsert(clusterRecord);
    verify(subject, times(1)).fireInform(any(), eq(clusterRecord));
  }

  @Test
  @Category(UnitTests.class)
  public void shouldNotInformUponUpdate() {
    clusterRecordService.upsert(clusterRecord);
    verify(subject, times(1)).fireInform(any(), eq(clusterRecord));
    clusterRecordService.upsert(clusterRecord);
    verify(subject, times(1)).fireInform(any(), eq(clusterRecord)); // instead of 2
  }

  @Test
  @Category(UnitTests.class)
  public void shouldDeleteExistingClusters() {
    clusterRecordService.upsert(clusterRecord);
    Boolean pass2 = clusterRecordService.delete(accountId, cloudProviderId);
    assertThat(pass2).isTrue();
  }
}
