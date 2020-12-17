package io.harness.registrars;

import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.sdk.registries.registrar.StepRegistrar;
import io.harness.states.BuildStatusStep;
import io.harness.states.BuildStep;
import io.harness.states.CIPipelineSetupStep;
import io.harness.states.CleanupStep;
import io.harness.states.DockerStep;
import io.harness.states.ECRStep;
import io.harness.states.GCRStep;
import io.harness.states.GitCloneStep;
import io.harness.states.IntegrationStageStep;
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

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

public class ExecutionRegistrar implements StepRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<StepType, Step>> stateClasses) {
    stateClasses.add(Pair.of(LiteEngineTaskStep.STEP_TYPE, injector.getInstance(LiteEngineTaskStep.class)));
    stateClasses.add(Pair.of(CleanupStep.STEP_TYPE, injector.getInstance(CleanupStep.class)));
    stateClasses.add(Pair.of(BuildStep.STEP_TYPE, injector.getInstance(BuildStep.class)));
    stateClasses.add(Pair.of(GitCloneStep.STEP_TYPE, injector.getInstance(GitCloneStep.class)));
    stateClasses.add(Pair.of(RunStep.STEP_TYPE, injector.getInstance(RunStep.class)));
    stateClasses.add(Pair.of(RestoreCacheStep.STEP_TYPE, injector.getInstance(RestoreCacheStep.class)));
    stateClasses.add(Pair.of(SaveCacheStep.STEP_TYPE, injector.getInstance(SaveCacheStep.class)));
    stateClasses.add(Pair.of(PublishStep.STEP_TYPE, injector.getInstance(PublishStep.class)));
    stateClasses.add(Pair.of(IntegrationStageStep.STEP_TYPE, injector.getInstance(IntegrationStageStep.class)));
    stateClasses.add(Pair.of(CIPipelineSetupStep.STEP_TYPE, injector.getInstance(CIPipelineSetupStep.class)));
    stateClasses.add(Pair.of(BuildStatusStep.STEP_TYPE, injector.getInstance(BuildStatusStep.class)));
    stateClasses.add(Pair.of(PluginStep.STEP_TYPE, injector.getInstance(PluginStep.class)));
    stateClasses.add(Pair.of(ECRStep.STEP_TYPE, injector.getInstance(ECRStep.class)));
    stateClasses.add(Pair.of(GCRStep.STEP_TYPE, injector.getInstance(GCRStep.class)));
    stateClasses.add(Pair.of(DockerStep.STEP_TYPE, injector.getInstance(DockerStep.class)));
    stateClasses.add(Pair.of(UploadToS3Step.STEP_TYPE, injector.getInstance(UploadToS3Step.class)));
    stateClasses.add(Pair.of(SaveCacheS3Step.STEP_TYPE, injector.getInstance(SaveCacheS3Step.class)));
    stateClasses.add(Pair.of(RestoreCacheS3Step.STEP_TYPE, injector.getInstance(RestoreCacheS3Step.class)));
    stateClasses.add(Pair.of(UploadToGCSStep.STEP_TYPE, injector.getInstance(UploadToGCSStep.class)));
    stateClasses.add(Pair.of(SaveCacheGCSStep.STEP_TYPE, injector.getInstance(SaveCacheGCSStep.class)));
    stateClasses.add(Pair.of(RestoreCacheGCSStep.STEP_TYPE, injector.getInstance(RestoreCacheGCSStep.class)));
    stateClasses.add(Pair.of(TestIntelligenceStep.STEP_TYPE, injector.getInstance(TestIntelligenceStep.class)));
  }
}
