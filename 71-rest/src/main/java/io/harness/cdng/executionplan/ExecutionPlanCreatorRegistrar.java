package io.harness.cdng.executionplan;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.pipeline.plancreators.DeploymentStagePlanCreator;
import io.harness.cdng.pipeline.plancreators.ExecutionPhasesPlanCreator;
import io.harness.cdng.pipeline.plancreators.HttpStepPlanCreator;
import io.harness.cdng.pipeline.plancreators.PhasePlanCreator;
import io.harness.cdng.pipeline.plancreators.PipelinePlanCreator;
import io.harness.cdng.pipeline.plancreators.StagesPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorRegistry;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreators.ParallelStepPlanCreator;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ExecutionPlanCreatorRegistrar {
  @Inject private ExecutionPlanCreatorRegistry executionPlanCreatorRegistry;
  @Inject private PipelinePlanCreator pipelinePlanCreator;
  @Inject private StagesPlanCreator stagesPlanCreator;
  @Inject private DeploymentStagePlanCreator deploymentStagePlanCreator;
  @Inject private ExecutionPhasesPlanCreator executionPhasesPlanCreator;
  @Inject private PhasePlanCreator phasePlanCreator;
  @Inject private HttpStepPlanCreator httpStepPlanCreator;
  @Inject private ParallelStepPlanCreator parallelStepPlanCreator;

  public void register() {
    logger.info("Start: register execution plan creators");
    register(pipelinePlanCreator);
    register(stagesPlanCreator);
    register(deploymentStagePlanCreator);
    register(executionPhasesPlanCreator);
    register(phasePlanCreator);
    register(httpStepPlanCreator);
    register(parallelStepPlanCreator);
    logger.info("Done: register execution plan creators");
  }
  private void register(SupportDefinedExecutorPlanCreator<?> executionPlanCreator) {
    executionPlanCreatorRegistry.registerCreator(executionPlanCreator, executionPlanCreator);
  }
}
