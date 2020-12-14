package io.harness.states;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static io.harness.rule.OwnerRule.HARSH;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.steps.stepinfo.BuildStepInfo;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.rule.Owner;
import io.harness.service.DelegateGrpcClientWrapper;
import io.harness.stateutils.buildstate.ConnectorUtils;

import com.google.inject.Inject;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class BuildStepTest extends CIExecutionTest {
  @Inject private CIExecutionPlanTestHelper ciExecutionPlanTestHelper;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @Mock private ConnectorUtils connectorUtils;
  @Mock private DelegateGrpcClientWrapper delegateGrpcClientWrapper;

  @InjectMocks private BuildStep buildStep;

  @Before
  public void setUp() {}

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldExecuteCIBuildTask() throws IOException {
    Ambiance ambiance = Ambiance.newBuilder().build();
    when(delegateGrpcClientWrapper.executeSyncTask(any())).thenReturn(K8sTaskExecutionResponse.builder().build());
    when(executionSweepingOutputResolver.resolve(any(), any()))
        .thenReturn(K8PodDetails.builder().clusterName("cluster").namespace("namespace").build());
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ConnectorDetails.builder().build());

    buildStep.executeSync(ambiance,
        BuildStepInfo.builder().scriptInfos(ciExecutionPlanTestHelper.getBuildCommandSteps()).build(), null, null);

    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(any());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldNotExecuteCIBuildTask() throws IOException {
    Ambiance ambiance = Ambiance.newBuilder().build();
    when(delegateGrpcClientWrapper.executeSyncTask(any())).thenReturn(K8sTaskExecutionResponse.builder().build());
    when(delegateGrpcClientWrapper.executeSyncTask(any())).thenThrow(new RuntimeException());
    when(executionSweepingOutputResolver.resolve(any(), any()))
        .thenReturn(K8PodDetails.builder().clusterName("cluster").namespace("namespace").build());
    when(connectorUtils.getConnectorDetails(any(), any())).thenReturn(ConnectorDetails.builder().build());

    buildStep.executeSync(ambiance,
        BuildStepInfo.builder().scriptInfos(ciExecutionPlanTestHelper.getBuildCommandSteps()).build(), null, null);

    verify(delegateGrpcClientWrapper, times(1)).executeSyncTask(any());
  }
}
