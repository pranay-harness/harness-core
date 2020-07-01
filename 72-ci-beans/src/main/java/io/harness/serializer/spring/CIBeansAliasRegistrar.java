package io.harness.serializer.spring;

import io.harness.beans.CIPipeline;
import io.harness.beans.CIPipelineEntityInfo;
import io.harness.beans.CIPipelineSetupParameters;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.BuildStepInfo;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.beans.steps.stepinfo.TestStepInfo;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.yaml.extended.container.Container;
import io.harness.spring.AliasRegistrar;

import java.util.Map;

/**
 * DO NOT CHANGE the keys. This is how track the Interface Implementations
 */
public class CIBeansAliasRegistrar implements AliasRegistrar {
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
    orchestrationElements.put("saveCacheStepInfo", SaveCacheStepInfo.class);
    orchestrationElements.put("saveCacheStepInfo_saveCache", SaveCacheStepInfo.SaveCache.class);
    orchestrationElements.put("restoreCacheStepInfo", RestoreCacheStepInfo.class);
    orchestrationElements.put("restoreCacheStepInfo_restoreCache", RestoreCacheStepInfo.RestoreCache.class);
    orchestrationElements.put("ciPipeline", CIPipeline.class);
    orchestrationElements.put("ciPipelineEntityInfo", CIPipelineEntityInfo.class);
    orchestrationElements.put("integrationStage_integration", IntegrationStage.Integration.class);
    orchestrationElements.put("buildEnvSetupStepInfo_buildEnvSetup", BuildEnvSetupStepInfo.BuildEnvSetup.class);
    orchestrationElements.put("runStepInfo_run", RunStepInfo.Run.class);
    orchestrationElements.put("typeInfo", TypeInfo.class);
    orchestrationElements.put("integrationStage", IntegrationStage.class);
    orchestrationElements.put("liteEngineTaskStepInfo_envSetupInfo", LiteEngineTaskStepInfo.EnvSetupInfo.class);
    orchestrationElements.put("container", Container.class);
    orchestrationElements.put("testStepInfo_test", TestStepInfo.Test.class);
    orchestrationElements.put("buildStepInfo_build", BuildStepInfo.Build.class);
    orchestrationElements.put("container_resources", Container.Resources.class);
    orchestrationElements.put("container_limit", Container.Limit.class);
    orchestrationElements.put("gitCloneStepInfo_gitClone", GitCloneStepInfo.GitClone.class);
    orchestrationElements.put("cleanUpStepInfo_cleanup", CleanupStepInfo.Cleanup.class);
    orchestrationElements.put("container_reserve", Container.Reserve.class);
  }
}
