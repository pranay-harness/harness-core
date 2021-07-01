package io.harness.stateutils.buildstate;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.executionplan.CIExecutionPlanTestHelper.GIT_CONNECTOR;
import static io.harness.rule.OwnerRule.HARSH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.category.element.UnitTests;
import io.harness.ci.beans.entities.LogServiceConfig;
import io.harness.ci.beans.entities.TIServiceConfig;
import io.harness.delegate.beans.ci.CIBuildSetupTaskParams;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.SecretVariableDetails;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTestBase;
import io.harness.logserviceclient.CILogServiceUtils;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.plan.ExecutionMetadata;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.rule.Owner;
import io.harness.tiserviceclient.TIServiceUtils;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.CI)
public class BuildSetupUtilsTest extends CIExecutionTestBase {
  @Inject private BuildSetupUtils buildSetupUtils;
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Inject private K8BuildSetupUtils k8BuildSetupUtils;
  @Mock private ConnectorUtils connectorUtils;
  @Mock private SecretUtils secretUtils;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Mock CILogServiceUtils logServiceUtils;
  @Mock TIServiceUtils tiServiceUtils;

  private static final String CLUSTER_NAME = "K8";

  @Before
  public void setUp() {
    on(buildSetupUtils).set("k8BuildSetupUtils", k8BuildSetupUtils);
    on(k8BuildSetupUtils).set("secretVariableUtils", secretUtils);
    on(k8BuildSetupUtils).set("connectorUtils", connectorUtils);
    on(k8BuildSetupUtils).set("executionSweepingOutputResolver", executionSweepingOutputResolver);
    on(k8BuildSetupUtils).set("logServiceUtils", logServiceUtils);
    on(k8BuildSetupUtils).set("tiServiceUtils", tiServiceUtils);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void shouldFetBuildSetupTaskParams() {
    int buildID = 1;
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("accountId", "account");
    setupAbstractions.put("projectIdentifier", "project");
    setupAbstractions.put("orgIdentifier", "org");
    ExecutionMetadata executionMetadata = ExecutionMetadata.newBuilder()
                                              .setExecutionUuid(generateUuid())
                                              .setRunSequence(buildID)
                                              .setPipelineIdentifier("pipeline")
                                              .build();
    Ambiance ambiance =
        Ambiance.newBuilder().putAllSetupAbstractions(setupAbstractions).setMetadata(executionMetadata).build();

    HashMap<String, String> taskIds = new HashMap<>();
    HashMap<String, String> logKeys = new HashMap<>();
    when(connectorUtils.getConnectorDetails(any(), eq(GIT_CONNECTOR)))
        .thenReturn(ciExecutionPlanTestHelper.getGitConnector());
    when(connectorUtils.getConnectorDetailsWithConversionInfo(any(), any()))
        .thenReturn(ConnectorDetails.builder().identifier("connectorId").build());

    when(secretUtils.getSecretVariableDetails(any(), any())).thenReturn(SecretVariableDetails.builder().build());
    LogServiceConfig logServiceConfig = LogServiceConfig.builder().baseUrl("endpoint").globalToken("token").build();
    when(logServiceUtils.getLogServiceConfig()).thenReturn(logServiceConfig);
    when(logServiceUtils.getLogServiceToken(any())).thenReturn("token");
    TIServiceConfig tiServiceConfig = TIServiceConfig.builder().baseUrl("endpoint").globalToken("token").build();
    when(tiServiceUtils.getTiServiceConfig()).thenReturn(tiServiceConfig);
    when(tiServiceUtils.getTIServiceToken(any())).thenReturn("token");
    when(executionSweepingOutputResolver.resolve(any(), any()))
        .thenReturn(K8PodDetails.builder().clusterName("cluster").namespace("namespace").stageID("stage").build());

    CIBuildSetupTaskParams buildSetupTaskParams = buildSetupUtils.getBuildSetupTaskParams(
        ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnFirstPodWithSetCallbackId(), ambiance, taskIds, "test",
        logKeys);
    assertThat(buildSetupTaskParams).isNotNull();
    verify(logServiceUtils, times(1)).getLogServiceConfig();
    verify(logServiceUtils, times(1)).getLogServiceToken(any());
    verify(tiServiceUtils, times(1)).getTiServiceConfig();
    verify(tiServiceUtils, times(1)).getTIServiceToken(any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("Recreate test object after pms integration")
  public void shouldFetBuildSetupTaskParamsWithAccountConnector() {
    int buildID = 1;
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("accountId", "account");
    setupAbstractions.put("projectIdentifier", "project");
    setupAbstractions.put("orgIdentifier", "org");
    ExecutionMetadata executionMetadata = ExecutionMetadata.newBuilder()
                                              .setExecutionUuid(generateUuid())
                                              .setRunSequence(buildID)
                                              .setPipelineIdentifier("pipeline")
                                              .build();
    Ambiance ambiance =
        Ambiance.newBuilder().putAllSetupAbstractions(setupAbstractions).setMetadata(executionMetadata).build();

    HashMap<String, String> taskIds = new HashMap<>();
    HashMap<String, String> logKeys = new HashMap<>();
    when(connectorUtils.getConnectorDetails(any(), eq(GIT_CONNECTOR)))
        .thenReturn(ciExecutionPlanTestHelper.getGitAccountConnector());
    when(connectorUtils.getConnectorDetailsWithConversionInfo(any(), any()))
        .thenReturn(ConnectorDetails.builder().identifier("connectorId").build());

    when(secretUtils.getSecretVariableDetails(any(), any())).thenReturn(SecretVariableDetails.builder().build());
    LogServiceConfig logServiceConfig = LogServiceConfig.builder().baseUrl("endpoint").globalToken("token").build();
    when(logServiceUtils.getLogServiceConfig()).thenReturn(logServiceConfig);
    when(logServiceUtils.getLogServiceToken(any())).thenReturn("token");
    TIServiceConfig tiServiceConfig = TIServiceConfig.builder().baseUrl("endpoint").globalToken("token").build();
    when(tiServiceUtils.getTiServiceConfig()).thenReturn(tiServiceConfig);
    when(tiServiceUtils.getTIServiceToken(any())).thenReturn("token");
    when(executionSweepingOutputResolver.resolve(any(), any()))
        .thenReturn(K8PodDetails.builder().clusterName("cluster").namespace("namespace").stageID("stage").build());

    CIBuildSetupTaskParams buildSetupTaskParams = buildSetupUtils.getBuildSetupTaskParams(
        ciExecutionPlanTestHelper.getExpectedLiteEngineTaskInfoOnFirstPodWithSetCallbackIdReponameSet(), ambiance,
        taskIds, "test", logKeys);
    assertThat(buildSetupTaskParams).isNotNull();
    verify(logServiceUtils, times(1)).getLogServiceConfig();
    verify(logServiceUtils, times(1)).getLogServiceToken(any());
    verify(tiServiceUtils, times(1)).getTiServiceConfig();
    verify(tiServiceUtils, times(1)).getTIServiceToken(any());
  }
}
