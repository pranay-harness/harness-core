package io.harness.stateutils.buildstate;

import static io.harness.executionplan.CIExecutionPlanTestHelper.GIT_CONNECTOR;
import static io.harness.rule.OwnerRule.HARSH;

import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.ci.beans.entities.LogServiceConfig;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.SecretVariableDetails;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.logserviceclient.CILogServiceUtils;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.expression.PmsEngineExpressionService;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class BuildSetupUtilsTest extends CIExecutionTest {
  @Inject private BuildSetupUtils buildSetupUtils;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Inject private K8BuildSetupUtils k8BuildSetupUtils;
  @Mock private ConnectorUtils connectorUtils;
  @Mock private SecretVariableUtils secretVariableUtils;
  @Mock private PmsEngineExpressionService pmsEngineExpressionService;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Mock CILogServiceUtils logServiceUtils;

  private static final String CLUSTER_NAME = "K8";

  @Before
  public void setUp() {
    on(buildSetupUtils).set("k8BuildSetupUtils", k8BuildSetupUtils);
    on(k8BuildSetupUtils).set("secretVariableUtils", secretVariableUtils);
    on(k8BuildSetupUtils).set("connectorUtils", connectorUtils);
    on(k8BuildSetupUtils).set("executionSweepingOutputResolver", executionSweepingOutputResolver);
    on(k8BuildSetupUtils).set("logServiceUtils", logServiceUtils);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldFetBuildSetupTaskParams() throws Exception {
    Ambiance ambiance = Ambiance.newBuilder().build();
    when(connectorUtils.getConnectorDetails(any(), eq(GIT_CONNECTOR)))
        .thenReturn(ciExecutionPlanTestHelper.getGitConnector());
    when(connectorUtils.getConnectorDetailsWithConversionInfo(any(), any()))
        .thenReturn(ConnectorDetails.builder().identifier("connectorId").build());

    when(secretVariableUtils.getSecretVariableDetails(any(), any()))
        .thenReturn(SecretVariableDetails.builder().build());
    LogServiceConfig logServiceConfig = LogServiceConfig.builder().baseUrl("endpoint").globalToken("token").build();
    when(logServiceUtils.getLogServiceConfig()).thenReturn(logServiceConfig);
    when(logServiceUtils.getLogServiceToken(any())).thenReturn("token");
    when(pmsEngineExpressionService.renderExpression(any(), any())).thenReturn(CLUSTER_NAME);
    when(executionSweepingOutputResolver.resolve(any(), any()))
        .thenReturn(K8PodDetails.builder()
                        .clusterName("cluster")
                        .namespace("namespace")
                        .buildNumberDetails(BuildNumberDetails.builder().buildNumber(1L).build())
                        .build());
    //
    //    CIBuildSetupTaskParams buildSetupTaskParams = buildSetupUtils.getBuildSetupTaskParams(
    //        ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnFirstPodWithSetCallbackId(), ambiance);
    //    assertThat(buildSetupTaskParams).isNotNull();
    //    verify(logServiceUtils, times(1)).getLogServiceConfig();
    //    verify(logServiceUtils, times(1)).getLogServiceToken(any());
  }
}
