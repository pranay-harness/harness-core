package io.harness.states;

import static io.harness.rule.OwnerRule.HARSH;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.category.element.UnitTests;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import io.harness.stateutils.buildstate.BuildSetupUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.helpers.ext.k8s.response.K8sTaskExecutionResponse;

import java.io.IOException;

public class BuildEnvSetupStepTest extends CIExecutionTest {
  @Inject private BuildEnvSetupStep buildEnvSetupStep;
  @Mock private BuildSetupUtils buildSetupUtils;
  @Mock private Ambiance ambiance;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;

  @Before
  public void setUp() {
    on(buildEnvSetupStep).set("buildSetupUtils", buildSetupUtils);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldExecuteCISetupTask() throws IOException {
    when(buildSetupUtils.executeCISetupTask(any(), any())).thenReturn(K8sTaskExecutionResponse.builder().build());
    when(executionSweepingOutputResolver.resolve(any(), any()))
        .thenReturn(K8PodDetails.builder().clusterName("cluster").namespace("namespace").build());

    buildEnvSetupStep.executeSync(null, BuildEnvSetupStepInfo.builder().build(), null, null);

    verify(buildSetupUtils, times(1)).executeCISetupTask(any(), any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldNotExecuteCISetupTask() throws IOException {
    when(buildSetupUtils.executeCISetupTask(any(), any())).thenThrow(new RuntimeException());
    buildEnvSetupStep.executeSync(ambiance, BuildEnvSetupStepInfo.builder().build(), null, null);

    verify(buildSetupUtils, times(1)).executeCISetupTask(any(), any());
  }
}
