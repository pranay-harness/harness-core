package io.harness.serializer.spring;

import io.harness.beans.CIPipelineSetupParameters;
import io.harness.beans.environment.K8BuildJobEnvInfo;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.stages.IntegrationStageStepParameters;
import io.harness.beans.steps.TypeInfo;
import io.harness.beans.steps.stepinfo.BuildEnvSetupStepInfo;
import io.harness.beans.steps.stepinfo.BuildStepInfo;
import io.harness.beans.steps.stepinfo.CleanupStepInfo;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.steps.stepinfo.LiteEngineTaskStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RestoreCacheStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.SaveCacheStepInfo;
import io.harness.beans.steps.stepinfo.TestStepInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.DockerFileArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.DockerImageArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.FilePatternArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.ArtifactoryConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.DockerhubConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.EcrConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.GcrConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.NexusConnector;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.S3Connector;
import io.harness.beans.sweepingoutputs.K8PodDetails;
import io.harness.beans.sweepingoutputs.StepTaskDetails;
import io.harness.beans.yaml.extended.CustomSecretVariable;
import io.harness.beans.yaml.extended.CustomTextVariable;
import io.harness.beans.yaml.extended.connector.GitConnectorYaml;
import io.harness.beans.yaml.extended.container.Container;
import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.beans.yaml.extended.infrastrucutre.UseFromStageInfraYaml;
import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.ci.stdvars.BuildStandardVariables;
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
    orchestrationElements.put("stepTaskDetails", StepTaskDetails.class);
    orchestrationElements.put("buildStandardVariables", BuildStandardVariables.class);
    orchestrationElements.put("liteEngineTaskStepInfo", LiteEngineTaskStepInfo.class);
    orchestrationElements.put("publishStepInfo", PublishStepInfo.class);
    orchestrationElements.put("runStepInfo", RunStepInfo.class);
    orchestrationElements.put("pluginStepInfo", PluginStepInfo.class);
    orchestrationElements.put("testStepInfo", TestStepInfo.class);
    orchestrationElements.put("saveCacheStepInfo", SaveCacheStepInfo.class);
    orchestrationElements.put("restoreCacheStepInfo", RestoreCacheStepInfo.class);
    orchestrationElements.put("buildNumberDetails", BuildNumberDetails.class);
    orchestrationElements.put("typeInfo", TypeInfo.class);
    orchestrationElements.put("integrationStage", IntegrationStage.class);
    orchestrationElements.put("container", Container.class);
    orchestrationElements.put("resource", ContainerResource.class);
    orchestrationElements.put("resource_limit", ContainerResource.Limit.class);
    orchestrationElements.put("container_resources", Container.Resources.class);
    orchestrationElements.put("container_limit", Container.Limit.class);
    orchestrationElements.put("container_reserve", Container.Reserve.class);
    orchestrationElements.put("k8sDirectInfraYaml", K8sDirectInfraYaml.class);
    orchestrationElements.put("useFromStageInfraYaml", UseFromStageInfraYaml.class);
    orchestrationElements.put("useFromStageInfraYaml_useFromStage", UseFromStageInfraYaml.UseFromStage.class);
    orchestrationElements.put("infrastructure_type", Infrastructure.Type.class);
    orchestrationElements.put("k8BuildJobEnvInfo", K8BuildJobEnvInfo.class);
    orchestrationElements.put("gitConnectorYaml", GitConnectorYaml.class);
    orchestrationElements.put("dockerFileArtifact", DockerFileArtifact.class);
    orchestrationElements.put("dockerImageArtifact", DockerImageArtifact.class);
    orchestrationElements.put("filePatternArtifact", FilePatternArtifact.class);
    orchestrationElements.put("artifactoryConnector", ArtifactoryConnector.class);
    orchestrationElements.put("dockerhubConnector", DockerhubConnector.class);
    orchestrationElements.put("ecrConnector", EcrConnector.class);
    orchestrationElements.put("gcrConnector", GcrConnector.class);
    orchestrationElements.put("nexusConnector", NexusConnector.class);
    orchestrationElements.put("s3Connector", S3Connector.class);
    orchestrationElements.put("customSecretVariable", CustomSecretVariable.class);
    orchestrationElements.put("customTextVariable", CustomTextVariable.class);
  }
}
