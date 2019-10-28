package io.harness.ccm;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static software.wings.beans.FeatureName.PERPETUAL_TASK_SERVICE;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.perpetualtask.k8s.watch.K8sWatchPerpetualTaskServiceClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.FeatureFlagService;

public class CCMPerpetualTaskHandlerTest extends WingsBaseTest {
  @Mock FeatureFlagService featureFlagService;
  @Mock K8sWatchPerpetualTaskServiceClient k8SWatchPerpetualTaskServiceClient;
  @InjectMocks CCMPerpetualTaskHandler handler;

  String accountId = "ACCOUNT_ID";
  private String cloudProviderId = "CLOUD_PROVIDER_ID";

  @Before
  public void setUp() {
    when(featureFlagService.isGlobalEnabled(PERPETUAL_TASK_SERVICE)).thenReturn(true);
    when(k8SWatchPerpetualTaskServiceClient.create(anyString(), any())).thenReturn("");
  }

  @Test
  @Category(UnitTests.class)
  public void shouldStartPerpetualTask() {
    Cluster k8sCluster = DirectKubernetesCluster.builder().cloudProviderId(cloudProviderId).build();
    ClusterRecord clusterRecord = ClusterRecord.builder().accountId(accountId).cluster(k8sCluster).build();
    handler.startPerpetualTask(clusterRecord);
    verify(k8SWatchPerpetualTaskServiceClient, times(2)).create(eq(accountId), any());
  }
}
