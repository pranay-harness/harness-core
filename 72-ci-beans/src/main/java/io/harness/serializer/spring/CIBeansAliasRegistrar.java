package io.harness.serializer.spring;

import io.harness.OrchestrationBeansAliasRegistrar;
import io.harness.beans.CIPipelineSetupParameters;
import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.BuildStepInfo;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.TestStepInfo;
import io.harness.beans.sweepingoutputs.K8PodDetails;

import java.util.Map;

/**
 * DO NOT CHANGE the keys. This is how track the Interface Implementations
 */
public class CIBeansAliasRegistrar implements OrchestrationBeansAliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("buildEnvSetupStepInfo", BuildEnvSetupStepInfo.class);
    orchestrationElements.put("buildStepInfo", BuildStepInfo.class);
    orchestrationElements.put("ciPipelineSetupParameters", CIPipelineSetupParameters.class);
    orchestrationElements.put("cleanupStepInfo", CleanupStepInfo.class);
    orchestrationElements.put("gitCloneStepInfo", GitCloneStepInfo.class);
    orchestrationElements.put("integrationStageStepParameters", IntegrationStageStepParameters.class);
    orchestrationElements.put("k8PodDetails", K8PodDetails.class);
    orchestrationElements.put("liteEngineTaskStepInfo", LiteEngineTaskStepInfo.class);
    orchestrationElements.put("publishStepInfo", PublishStepInfo.class);
    orchestrationElements.put("runStepInfo", RunStepInfo.class);
    orchestrationElements.put("testStepInfo", TestStepInfo.class);
  }
}
