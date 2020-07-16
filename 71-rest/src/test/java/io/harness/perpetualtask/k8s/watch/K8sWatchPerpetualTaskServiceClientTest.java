package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.perpetualtask.PerpetualTaskClientContext;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.service.intfc.SettingsService;

import java.util.HashMap;
import java.util.Map;

public class K8sWatchPerpetualTaskServiceClientTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String cloudProviderId = "CLOUD_PROVIDER_ID";
  private String taskId = "TASK_ID";

  private static final String CLOUD_PROVIDER_ID = "cloudProviderId";
  private static final String CLUSTER_ID = "clusterId";
  private static final String CLUSTER_NAME = "clusterName";
  private Map<String, String> clientParamsMap = new HashMap<>();

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  @Mock SettingsService settingsService;
  @Mock K8sClusterConfigFactory k8sClusterConfigFactory;
  @InjectMocks K8sWatchPerpetualTaskServiceClient k8SWatchPerpetualTaskServiceClient;

  @Before
  public void setUp() {
    when(settingsService.get(CLOUD_PROVIDER_ID)).thenReturn(null);
    when(k8sClusterConfigFactory.getK8sClusterConfig(any())).thenReturn(null);

    clientParamsMap.put(CLOUD_PROVIDER_ID, CLOUD_PROVIDER_ID);
    clientParamsMap.put(CLUSTER_NAME, CLUSTER_NAME);
    clientParamsMap.put(CLUSTER_ID, CLUSTER_ID);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetTaskParams() {
    PerpetualTaskClientContext perpetualTaskClientContext =
        PerpetualTaskClientContext.builder().clientParams(clientParamsMap).build();
    K8sWatchTaskParams k8sWatchTaskParams =
        k8SWatchPerpetualTaskServiceClient.getTaskParams(perpetualTaskClientContext);
    assertThat(k8sWatchTaskParams.getClusterId()).isEqualTo(CLUSTER_ID);
    assertThat(k8sWatchTaskParams.getClusterName()).isEqualTo(CLUSTER_NAME);
    assertThat(k8sWatchTaskParams.getCloudProviderId()).isEqualTo(CLOUD_PROVIDER_ID);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void testGetValidationTask() {
    PerpetualTaskClientContext perpetualTaskClientContext =
        PerpetualTaskClientContext.builder().clientParams(clientParamsMap).build();
    DelegateTask delegateTask =
        k8SWatchPerpetualTaskServiceClient.getValidationTask(perpetualTaskClientContext, accountId);
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getAccountId()).isEqualTo(accountId);
  }
}
