package io.harness.registrars;

import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.states.BuildStatusStep;
import io.harness.states.BuildStep;
import io.harness.states.CIPipelineSetupStep;
import io.harness.states.CleanupStep;
import io.harness.states.DockerStep;
import io.harness.states.ECRStep;
import io.harness.states.GCRStep;
import io.harness.states.GitCloneStep;
import io.harness.states.IntegrationStageStep;
import io.harness.states.IntegrationStageStepPMS;
import io.harness.states.LiteEngineTaskStep;
import io.harness.states.PluginStep;
import io.harness.states.PublishStep;
import io.harness.states.RestoreCacheGCSStep;
import io.harness.states.RestoreCacheS3Step;
import io.harness.states.RestoreCacheStep;
import io.harness.states.RunStep;
import io.harness.states.SaveCacheGCSStep;
import io.harness.states.SaveCacheS3Step;
import io.harness.states.SaveCacheStep;
import io.harness.states.TestIntelligenceStep;
import io.harness.states.UploadToGCSStep;
import io.harness.states.UploadToS3Step;

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExecutionRegistrar {
  public Map<StepType, Class<? extends Step>> getEngineSteps() {
    Map<StepType, Class<? extends Step>> engineSteps = new HashMap<>();

    engineSteps.put(LiteEngineTaskStep.STEP_TYPE, LiteEngineTaskStep.class);
    engineSteps.put(CleanupStep.STEP_TYPE, CleanupStep.class);
    engineSteps.put(BuildStep.STEP_TYPE, BuildStep.class);
    engineSteps.put(GitCloneStep.STEP_TYPE, GitCloneStep.class);
    engineSteps.put(RunStep.STEP_TYPE, RunStep.class);
    engineSteps.put(RestoreCacheStep.STEP_TYPE, RestoreCacheStep.class);
    engineSteps.put(SaveCacheStep.STEP_TYPE, SaveCacheStep.class);
    engineSteps.put(PublishStep.STEP_TYPE, PublishStep.class);
    engineSteps.put(IntegrationStageStep.STEP_TYPE, IntegrationStageStep.class);
    engineSteps.put(CIPipelineSetupStep.STEP_TYPE, CIPipelineSetupStep.class);
    engineSteps.put(BuildStatusStep.STEP_TYPE, BuildStatusStep.class);
    engineSteps.put(PluginStep.STEP_TYPE, PluginStep.class);
    engineSteps.put(ECRStep.STEP_TYPE, ECRStep.class);
    engineSteps.put(GCRStep.STEP_TYPE, GCRStep.class);
    engineSteps.put(DockerStep.STEP_TYPE, DockerStep.class);
    engineSteps.put(UploadToS3Step.STEP_TYPE, UploadToS3Step.class);
    engineSteps.put(SaveCacheS3Step.STEP_TYPE, SaveCacheS3Step.class);
    engineSteps.put(RestoreCacheS3Step.STEP_TYPE, RestoreCacheS3Step.class);
    engineSteps.put(UploadToGCSStep.STEP_TYPE, UploadToGCSStep.class);
    engineSteps.put(SaveCacheGCSStep.STEP_TYPE, SaveCacheGCSStep.class);
    engineSteps.put(RestoreCacheGCSStep.STEP_TYPE, RestoreCacheGCSStep.class);
    engineSteps.put(TestIntelligenceStep.STEP_TYPE, TestIntelligenceStep.class);
    engineSteps.put(IntegrationStageStepPMS.STEP_TYPE, IntegrationStageStepPMS.class);
    engineSteps.putAll(OrchestrationStepsModuleStepRegistrar.getEngineSteps());
    return engineSteps;
  }
}
