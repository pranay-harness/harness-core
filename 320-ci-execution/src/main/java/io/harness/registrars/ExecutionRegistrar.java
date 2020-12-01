package io.harness.registrars;

import io.harness.pms.sdk.core.steps.Step;
import io.harness.pms.steps.StepType;
import io.harness.registries.registrar.StepRegistrar;
import io.harness.states.BuildStatusStep;
import io.harness.states.BuildStep;
import io.harness.states.CIPipelineSetupStep;
import io.harness.states.CleanupStep;
import io.harness.states.GitCloneStep;
import io.harness.states.IntegrationStageStep;
import io.harness.states.LiteEngineTaskStep;
import io.harness.states.PluginStep;
import io.harness.states.PublishStep;
import io.harness.states.RestoreCacheStep;
import io.harness.states.RunStep;
import io.harness.states.SaveCacheStep;

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
  }
}
