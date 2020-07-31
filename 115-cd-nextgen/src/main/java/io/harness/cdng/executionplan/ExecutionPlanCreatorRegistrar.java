package io.harness.cdng.executionplan;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.pipeline.plancreators.ArtifactForkPlanCreator;
import io.harness.cdng.pipeline.plancreators.ArtifactStepPlanCreator;
import io.harness.cdng.pipeline.plancreators.CDExecutionPlanCreator;
import io.harness.cdng.pipeline.plancreators.DeploymentStagePlanCreator;
import io.harness.cdng.pipeline.plancreators.InfraPlanCreator;
import io.harness.cdng.pipeline.plancreators.ManifestStepPlanCreator;
import io.harness.cdng.pipeline.plancreators.PipelinePlanCreator;
import io.harness.cdng.pipeline.plancreators.ServiceStepPlanCreator;
import io.harness.executionplan.core.ExecutionPlanCreatorRegistry;
import io.harness.executionplan.core.SupportDefinedExecutorPlanCreator;
import io.harness.executionplan.plancreator.GenericStepPlanCreator;
import io.harness.executionplan.plancreator.ParallelStagePlanCreator;
import io.harness.executionplan.plancreator.ParallelStepPlanCreator;
import io.harness.executionplan.plancreator.StagesPlanCreator;
import io.harness.executionplan.plancreator.StepGroupPlanCreator;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ExecutionPlanCreatorRegistrar {
  @Inject private ExecutionPlanCreatorRegistry executionPlanCreatorRegistry;
  @Inject private PipelinePlanCreator pipelinePlanCreator;
  @Inject private StagesPlanCreator stagesPlanCreator;
  @Inject private DeploymentStagePlanCreator deploymentStagePlanCreator;
  @Inject private CDExecutionPlanCreator cdExecutionPlanCreator;
  @Inject private GenericStepPlanCreator genericStepPlanCreator;
  @Inject private StepGroupPlanCreator stepGroupPlanCreator;
  @Inject private ParallelStepPlanCreator parallelStepPlanCreator;
  @Inject private ParallelStagePlanCreator parallelStagePlanCreator;
  @Inject private ArtifactStepPlanCreator artifactStepPlanCreator;
  @Inject private ManifestStepPlanCreator manifestStepPlanCreator;
  @Inject private ServiceStepPlanCreator serviceStepPlanCreator;
  @Inject private InfraPlanCreator infraPlanCreator;
  @Inject private ArtifactForkPlanCreator artifactForkPlanCreator;

  public void register() {
    logger.info("Start: register execution plan creators");
    register(pipelinePlanCreator);
    register(deploymentStagePlanCreator);
    register(cdExecutionPlanCreator);
    register(parallelStepPlanCreator);
    register(artifactStepPlanCreator);
    register(manifestStepPlanCreator);
    register(serviceStepPlanCreator);
    register(infraPlanCreator);
    register(artifactForkPlanCreator);
    register(stagesPlanCreator);
    register(genericStepPlanCreator);
    register(stepGroupPlanCreator);
    register(parallelStagePlanCreator);
    logger.info("Done: register execution plan creators");
  }
  private void register(SupportDefinedExecutorPlanCreator<?> executionPlanCreator) {
    executionPlanCreatorRegistry.registerCreator(executionPlanCreator, executionPlanCreator);
  }
}
