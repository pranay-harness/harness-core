package io.harness.ccm;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.DirectKubernetesCluster;
import io.harness.ccm.config.CCMSettingService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

public class CCMPerpetualTaskHandlerTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";

  private String cloudProviderId = "CLOUD_PROVIDER_ID";
  private static final String masterUrl = "dummyMasterUrl";
  public static final String username = "dummyUsername";
  public static final String password = "dummyPassword";

  private Cluster k8sCluster;
  private ClusterRecord clusterRecord;

  @Mock CCMSettingService ccmSettingService;
  @Mock private CCMPerpetualTaskManager ccmPerpetualTaskManager;
  @Inject ClusterRecordService clusterRecordService;
  @Inject @InjectMocks CCMPerpetualTaskHandler handler;

  @Before
  public void setUp() {
    k8sCluster = DirectKubernetesCluster.builder().cloudProviderId(cloudProviderId).build();
    clusterRecord = ClusterRecord.builder().accountId(accountId).cluster(k8sCluster).build();

    when(ccmSettingService.isCloudCostEnabled(isA(ClusterRecord.class))).thenReturn(true);
    when(ccmPerpetualTaskManager.createPerpetualTasks(isA(ClusterRecord.class))).thenReturn(true);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testOnUpserted() {
    handler.onUpserted(clusterRecord);
    verify(ccmPerpetualTaskManager).createPerpetualTasks(eq(clusterRecord));
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void testOnDeleting() {
    handler.onDeleting(clusterRecord);
    verify(ccmPerpetualTaskManager).deletePerpetualTasks(eq(clusterRecord));
  }
}
