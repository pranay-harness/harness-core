package io.harness.states;

import static io.harness.rule.OwnerRule.ALEKSANDAR;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.HDelegateTask;
import io.harness.engine.outputs.ExecutionSweepingOutputService;
import io.harness.execution.status.Status;
import io.harness.executionplan.CIExecutionPlanTestHelper;
import io.harness.executionplan.CIExecutionTest;
import io.harness.rule.Owner;
import io.harness.state.io.StepResponse;
import io.harness.stateutils.buildstate.BuildSetupUtils;
import io.harness.tasks.TaskExecutor;
import io.harness.tasks.TaskMode;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.core.StepGroupElement;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

public class LiteEngineTaskStepTest extends CIExecutionTest {
  @Inject CIExecutionPlanTestHelper executionPlanTestHelper;
  @Mock private BuildSetupUtils buildSetupUtils;
  @Mock private Map<String, TaskExecutor<HDelegateTask>> taskExecutorMap;
  @Mock private ExecutionSweepingOutputService executionSweepingOutputResolver;
  @InjectMocks private LiteEngineTaskStep liteEngineTaskStep;

  private Ambiance ambiance;

  @Before
  public void setUp() {
    Map<String, String> setupAbstractions = new HashMap<>();
    setupAbstractions.put("accountId", "accountId");
    ambiance = Ambiance.builder().setupAbstractions(setupAbstractions).build();
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldExecuteSync() {
    LiteEngineTaskStepInfo liteEngineTaskStepInfo =
        LiteEngineTaskStepInfo.builder()
            .steps(
                ExecutionElement.builder()
                    .steps(asList(StepElement.builder()
                                      .type("restoreCache")
                                      .stepSpecType(RestoreCacheStepInfo.builder().identifier("restoreCache").build())
                                      .build(),
                        StepElement.builder()
                            .type("run")
                            .stepSpecType(RunStepInfo.builder().identifier("run").build())
                            .build(),
                        ParallelStepElement.builder()
                            .sections(asList(StepElement.builder()
                                                 .type("publishArtifacts")
                                                 .stepSpecType(PublishStepInfo.builder().identifier("publish").build())
                                                 .build(),
                                StepElement.builder()
                                    .type("saveCache")
                                    .stepSpecType(SaveCacheStepInfo.builder().identifier("saveCache").build())
                                    .build()))
                            .build()))
                    .build())
            .build();

    TaskExecutor<HDelegateTask> executor = mock(TaskExecutor.class);
    when(taskExecutorMap.get(TaskMode.DELEGATE_TASK_V3.name())).thenReturn(executor);
    when(executor.queueTask(eq(ambiance.getSetupAbstractions()), any())).thenReturn("taskId");

    StepResponse stepResponse = liteEngineTaskStep.executeSync(ambiance, liteEngineTaskStepInfo, null, null);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
    verify(executor, times(4)).queueTask(eq(ambiance.getSetupAbstractions()), any());
  }

  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void shouldCatchExceptionWithUnsupportedStepElementExecuteSync() {
    LiteEngineTaskStepInfo liteEngineTaskStepInfo =
        LiteEngineTaskStepInfo.builder()
            .steps(ExecutionElement.builder().steps(singletonList(StepGroupElement.builder().build())).build())
            .build();

    TaskExecutor<HDelegateTask> executor = mock(TaskExecutor.class);
    when(taskExecutorMap.get(TaskMode.DELEGATE_TASK_V3.name())).thenReturn(executor);
    when(executor.queueTask(eq(ambiance.getSetupAbstractions()), any())).thenReturn("taskId");

    StepResponse stepResponse = liteEngineTaskStep.executeSync(ambiance, liteEngineTaskStepInfo, null, null);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.SUCCEEDED);
  }
}