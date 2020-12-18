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

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ExecutionRegistrar {
  public Map<StepType, Step> getEngineSteps(Injector injector) {
    Map<StepType, Step> engineSteps = new HashMap<>();

    engineSteps.put(LiteEngineTaskStep.STEP_TYPE, injector.getInstance(LiteEngineTaskStep.class));
    engineSteps.put(CleanupStep.STEP_TYPE, injector.getInstance(CleanupStep.class));
    engineSteps.put(BuildStep.STEP_TYPE, injector.getInstance(BuildStep.class));
    engineSteps.put(GitCloneStep.STEP_TYPE, injector.getInstance(GitCloneStep.class));
    engineSteps.put(RunStep.STEP_TYPE, injector.getInstance(RunStep.class));
    engineSteps.put(RestoreCacheStep.STEP_TYPE, injector.getInstance(RestoreCacheStep.class));
    engineSteps.put(SaveCacheStep.STEP_TYPE, injector.getInstance(SaveCacheStep.class));
    engineSteps.put(PublishStep.STEP_TYPE, injector.getInstance(PublishStep.class));
    engineSteps.put(IntegrationStageStep.STEP_TYPE, injector.getInstance(IntegrationStageStep.class));
    engineSteps.put(CIPipelineSetupStep.STEP_TYPE, injector.getInstance(CIPipelineSetupStep.class));
    engineSteps.put(BuildStatusStep.STEP_TYPE, injector.getInstance(BuildStatusStep.class));
    engineSteps.put(PluginStep.STEP_TYPE, injector.getInstance(PluginStep.class));
    engineSteps.put(ECRStep.STEP_TYPE, injector.getInstance(ECRStep.class));
    engineSteps.put(GCRStep.STEP_TYPE, injector.getInstance(GCRStep.class));
    engineSteps.put(DockerStep.STEP_TYPE, injector.getInstance(DockerStep.class));
    engineSteps.put(UploadToS3Step.STEP_TYPE, injector.getInstance(UploadToS3Step.class));
    engineSteps.put(SaveCacheS3Step.STEP_TYPE, injector.getInstance(SaveCacheS3Step.class));
    engineSteps.put(RestoreCacheS3Step.STEP_TYPE, injector.getInstance(RestoreCacheS3Step.class));
    engineSteps.put(UploadToGCSStep.STEP_TYPE, injector.getInstance(UploadToGCSStep.class));
    engineSteps.put(SaveCacheGCSStep.STEP_TYPE, injector.getInstance(SaveCacheGCSStep.class));
    engineSteps.put(RestoreCacheGCSStep.STEP_TYPE, injector.getInstance(RestoreCacheGCSStep.class));
    engineSteps.put(TestIntelligenceStep.STEP_TYPE, injector.getInstance(TestIntelligenceStep.class));
    engineSteps.put(IntegrationStageStepPMS.STEP_TYPE, injector.getInstance(IntegrationStageStepPMS.class));

    return engineSteps;
  }
}
