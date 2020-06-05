package io.harness.executionplan;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.executionplan.core.ExecutionPlanCreatorRegistry;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.ParallelStepPlanCreator;
import io.harness.executionplan.plancreator.StagesPlanCreator;
import io.harness.plancreators.CIPipelinePlanCreator;
import io.harness.plancreators.ExecutionPlanCreator;
import io.harness.plancreators.GenericStepPlanCreator;
import io.harness.plancreators.IntegrationStagePlanCreator;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CIExecutionPlanCreatorRegistrar {
  @Inject private ExecutionPlanCreatorRegistry executionPlanCreatorRegistry;
  @Inject private GenericStepPlanCreator genericStepPlanCreator;
  @Inject private CIPipelinePlanCreator ciPipelinePlanCreator;
  @Inject private IntegrationStagePlanCreator integrationStagePlanCreator;
  @Inject private ExecutionPlanCreator executionPlanCreator;
  @Inject private ParallelStepPlanCreator parallelStepPlanCreator;
  @Inject private StagesPlanCreator stagesPlanCreator;

  public void register() {
    logger.info("Start: register execution plan creators");
    register(integrationStagePlanCreator);
    register(ciPipelinePlanCreator);
    register(genericStepPlanCreator);
    register(executionPlanCreator);
    register(parallelStepPlanCreator);
    register(stagesPlanCreator);
    logger.info("Done: register execution plan creators");
  }
  private void register(SupportDefinedExecutorPlanCreator<?> executionPlanCreator) {
    executionPlanCreatorRegistry.registerCreator(executionPlanCreator, executionPlanCreator);
  }
}
