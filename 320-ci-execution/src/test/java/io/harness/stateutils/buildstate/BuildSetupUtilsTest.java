package io.harness.stateutils.buildstate;

import static io.harness.executionplan.CIExecutionPlanTestHelper.GIT_CONNECTOR;
import static io.harness.rule.OwnerRule.HARSH;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.ci.beans.entities.LogServiceConfig;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.engine.expressions.EngineExpressionService;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.logserviceclient.CILogServiceUtils;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.rule.Owner;
import io.harness.service.DelegateGrpcClientWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;
import software.wings.beans.ci.pod.SecretVariableDetails;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.util.Optional;

public class BuildSetupUtilsTest extends CIExecutionTest {
  @Inject private BuildSetupUtils buildSetupUtils;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Inject private K8BuildSetupUtils k8BuildSetupUtils;
  @Mock private ConnectorUtils connectorUtils;
  @Mock private SecretVariableUtils secretVariableUtils;
  @Mock private EngineExpressionService engineExpressionService;
  @Mock private Ambiance ambiance;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Mock private DelegateGrpcClientWrapper delegateGrpcClientWrapper;
  @Mock CILogServiceUtils logServiceUtils;

  private static final String CLUSTER_NAME = "K8";

  @Before
  public void setUp() {
    on(buildSetupUtils).set("k8BuildSetupUtils", k8BuildSetupUtils);
    on(k8BuildSetupUtils).set("delegateGrpcClientWrapper", delegateGrpcClientWrapper);
    on(k8BuildSetupUtils).set("secretVariableUtils", secretVariableUtils);
    on(k8BuildSetupUtils).set("connectorUtils", connectorUtils);
    on(k8BuildSetupUtils).set("executionSweepingOutputResolver", executionSweepingOutputResolver);
    on(k8BuildSetupUtils).set("logServiceUtils", logServiceUtils);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldExecuteCILiteEngineTask() throws Exception {
    BuildNumberDetails buildNumberDetails = BuildNumberDetails.builder().buildNumber(1L).build();
    Call<ResponseDTO<Optional<ConnectorDTO>>> connectorRequest = mock(Call.class);
    when(connectorRequest.execute())
        .thenReturn(
            Response.success(ResponseDTO.newResponse(Optional.of(ciExecutionPlanTestHelper.getDockerConnectorDTO()))));

    when(delegateGrpcClientWrapper.executeSyncTask(any())).thenReturn(K8sTaskExecutionResponse.builder().build());
    when(connectorUtils.getConnectorDetails(any(), eq(GIT_CONNECTOR)))
        .thenReturn(ciExecutionPlanTestHelper.getGitConnector());
    when(secretVariableUtils.getSecretVariableDetails(any(), any()))
        .thenReturn(SecretVariableDetails.builder().build());
    LogServiceConfig logServiceConfig = LogServiceConfig.builder().baseUrl("endpoint").globalToken("token").build();
    when(logServiceUtils.getLogServiceConfig()).thenReturn(logServiceConfig);
    when(logServiceUtils.getLogServiceToken(any())).thenReturn("token");
    when(engineExpressionService.renderExpression(any(), any())).thenReturn(CLUSTER_NAME);
    when(executionSweepingOutputResolver.resolve(any(), any()))
        .thenReturn(K8PodDetails.builder()
                        .clusterName("cluster")
                        .namespace("namespace")
                        .buildNumberDetails(buildNumberDetails)
                        .build());

    buildSetupUtils.executeCILiteEngineTask(
        ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnFirstPodWithSetCallbackId(), ambiance);

    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(any());
    verify(logServiceUtils, times(1)).getLogServiceConfig();
    verify(logServiceUtils, times(1)).getLogServiceToken(any());
  }
}
