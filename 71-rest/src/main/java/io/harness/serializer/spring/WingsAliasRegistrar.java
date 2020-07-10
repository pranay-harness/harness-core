package io.harness.serializer.spring;

import io.harness.cdng.artifact.bean.DockerArtifactOutcome;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.artifact.bean.yaml.ArtifactOverrideSets;
import io.harness.cdng.artifact.bean.yaml.DockerHubArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.GcrArtifactConfig;
import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.artifact.steps.ArtifactStepParameters;
import io.harness.cdng.environment.steps.EnvironmentStepParameters;
import io.harness.cdng.environment.yaml.EnvironmentYaml;
import io.harness.cdng.infra.InfrastructureSpec;
import io.harness.cdng.infra.beans.InfraUseFromStage;
import io.harness.cdng.infra.beans.K8sDirectInfraMapping;
import io.harness.cdng.infra.steps.InfraStepParameters;
import io.harness.cdng.infra.yaml.K8SDirectInfrastructure;
import io.harness.cdng.k8s.K8sRollingOutcome;
import io.harness.cdng.k8s.K8sRollingRollbackStepInfo;
import io.harness.cdng.k8s.K8sRollingRollbackStepParameters;
import io.harness.cdng.k8s.K8sRollingStepInfo;
import io.harness.cdng.k8s.K8sRollingStepParameters;
import io.harness.cdng.k8s.K8sRollingStepPassThroughData;
import io.harness.cdng.manifest.state.ManifestStepParameters;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.ManifestConfig;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.ManifestOverrideSets;
import io.harness.cdng.manifest.yaml.kinds.K8sManifest;
import io.harness.cdng.manifest.yaml.kinds.ValuesManifest;
import io.harness.cdng.pipeline.CDPhase;
import io.harness.cdng.pipeline.CDPipeline;
import io.harness.cdng.pipeline.DeploymentStage;
import io.harness.cdng.pipeline.PipelineInfrastructure;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.cdng.pipeline.beans.DeploymentStageStepParameters;
import io.harness.cdng.pipeline.stepinfo.HttpStepInfo;
import io.harness.cdng.pipeline.stepinfo.ShellScriptStepInfo;
import io.harness.cdng.service.ServiceConfig;
import io.harness.cdng.service.ServiceSpec;
import io.harness.cdng.service.StageOverridesConfig;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.beans.ServiceUseFromStage;
import io.harness.cdng.service.beans.ServiceUseFromStage.Overrides;
import io.harness.cdng.service.steps.ServiceStepParameters;
import io.harness.cdng.tasks.manifestFetch.step.ManifestFetchOutcome;
import io.harness.cdng.tasks.manifestFetch.step.ManifestFetchParameters;
import io.harness.cdng.variables.StageVariables;
import io.harness.redesign.advisers.HttpResponseCodeSwitchAdviserParameters;
import io.harness.redesign.states.email.EmailStepParameters;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import io.harness.redesign.states.http.chain.BasicHttpChainStepParameters;
import io.harness.redesign.states.shell.ShellScriptStepParameters;
import io.harness.redesign.states.shell.ShellScriptVariablesSweepingOutput;
import io.harness.redesign.states.wait.WaitStepParameters;
import io.harness.spring.AliasRegistrar;
import software.wings.api.ContainerRollbackRequestElement;
import software.wings.api.ContainerServiceElement;
import software.wings.api.HttpStateExecutionData;
import software.wings.api.PhaseExecutionData;
import software.wings.api.ScriptStateExecutionData;
import software.wings.api.ServiceInstanceIdsParam;
import software.wings.api.SimpleWorkflowParam;
import software.wings.api.TerraformApplyMarkerParam;
import software.wings.api.WaitStateExecutionData;
import software.wings.api.artifact.ServiceArtifactElements;
import software.wings.api.artifact.ServiceArtifactVariableElements;
import software.wings.api.ecs.EcsBGSetupData;
import software.wings.api.instancedetails.InstanceInfoVariables;
import software.wings.api.k8s.K8sHelmDeploymentElement;
import software.wings.api.pcf.DeploySweepingOutputPcf;
import software.wings.api.pcf.InfoVariables;
import software.wings.api.pcf.SetupSweepingOutputPcf;
import software.wings.api.pcf.SwapRouteRollbackSweepingOutputPcf;
import software.wings.beans.InfraMappingSweepingOutput;
import software.wings.beans.PcfConfig;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfRouteUpdateRequestConfigData;
import software.wings.helpers.ext.pcf.response.PcfAppSetupTimeDetails;
import software.wings.sm.PhaseExecutionSummary;

import java.util.Map;

/**
 * DO NOT CHANGE the keys. This is how track the Interface Implementations
 */

public class WingsAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("artifactListConfig", ArtifactListConfig.class);
    orchestrationElements.put("artifactStepParameters", ArtifactStepParameters.class);
    orchestrationElements.put("basicHttpChainStepParameters", BasicHttpChainStepParameters.class);
    orchestrationElements.put("basicHttpStepParameters", BasicHttpStepParameters.class);
    orchestrationElements.put("cdPipelineSetupParameters", CDPipelineSetupParameters.class);
    orchestrationElements.put("containerRollbackRequestElement", ContainerRollbackRequestElement.class);
    orchestrationElements.put("containerServiceElement", ContainerServiceElement.class);
    orchestrationElements.put("deploySweepingOutputPcf", DeploySweepingOutputPcf.class);
    orchestrationElements.put("deploymentStageStepParameters", DeploymentStageStepParameters.class);
    orchestrationElements.put("dockerArtifactOutcome", DockerArtifactOutcome.class);
    orchestrationElements.put("emailStepParameters", EmailStepParameters.class);
    orchestrationElements.put("environmentYaml", EnvironmentYaml.class);
    orchestrationElements.put("httpResponseCodeSwitchAdviserParameters", HttpResponseCodeSwitchAdviserParameters.class);
    orchestrationElements.put("httpStateExecutionData", HttpStateExecutionData.class);
    orchestrationElements.put("httpStepInfo", HttpStepInfo.class);
    orchestrationElements.put("infoVariables", InfoVariables.class);
    orchestrationElements.put("infraMappingSweepingOutput", InfraMappingSweepingOutput.class);
    orchestrationElements.put("instanceInfoVariables", InstanceInfoVariables.class);
    orchestrationElements.put("k8sDirectInfraMapping", K8sDirectInfraMapping.class);
    orchestrationElements.put("k8sDirectInfrastructure", K8SDirectInfrastructure.class);
    orchestrationElements.put("k8sHelmDeploymentElement", K8sHelmDeploymentElement.class);
    orchestrationElements.put("k8sRollingOutcome", K8sRollingOutcome.class);
    orchestrationElements.put("k8sRollingStepPassThroughData", K8sRollingStepPassThroughData.class);
    orchestrationElements.put("k8sRollingRollback", K8sRollingRollbackStepInfo.class);
    orchestrationElements.put("k8sRollingRollbackStepParameters", K8sRollingRollbackStepParameters.class);
    orchestrationElements.put("k8sRollingStepInfo", K8sRollingStepInfo.class);
    orchestrationElements.put("k8sRollingStepParameters", K8sRollingStepParameters.class);
    orchestrationElements.put("manifestFetchOutcome", ManifestFetchOutcome.class);
    orchestrationElements.put("manifestFetchParameters", ManifestFetchParameters.class);
    orchestrationElements.put("manifestOutcome", ManifestOutcome.class);
    orchestrationElements.put("manifestStepParameters", ManifestStepParameters.class);
    orchestrationElements.put("phaseExecutionData", PhaseExecutionData.class);
    orchestrationElements.put("phaseExecutionSummary", PhaseExecutionSummary.class);
    orchestrationElements.put("pipelineInfrastructure", PipelineInfrastructure.class);
    orchestrationElements.put("scriptStateExecutionData", ScriptStateExecutionData.class);
    orchestrationElements.put("serviceArtifactElements", ServiceArtifactElements.class);
    orchestrationElements.put("serviceArtifactVariableElements", ServiceArtifactVariableElements.class);
    orchestrationElements.put("serviceConfig", ServiceConfig.class);
    orchestrationElements.put("serviceInstanceIdsParam", ServiceInstanceIdsParam.class);
    orchestrationElements.put("serviceOutcome", ServiceOutcome.class);
    orchestrationElements.put("serviceStepParameters", ServiceStepParameters.class);
    orchestrationElements.put("setupSweepingOutputPcf", SetupSweepingOutputPcf.class);
    orchestrationElements.put("shellScriptStepInfo", ShellScriptStepInfo.class);
    orchestrationElements.put("shellScriptStepParameters", ShellScriptStepParameters.class);
    orchestrationElements.put("shellScriptVariablesSweepingOutput", ShellScriptVariablesSweepingOutput.class);
    orchestrationElements.put("simpleWorkflowParam", SimpleWorkflowParam.class);
    orchestrationElements.put("swapRouteRollbackSweepingOutputPcf", SwapRouteRollbackSweepingOutputPcf.class);
    orchestrationElements.put("terraformApplyMarkerParam", TerraformApplyMarkerParam.class);
    orchestrationElements.put("waitStateExecutionData", WaitStateExecutionData.class);
    orchestrationElements.put("waitStepParameters", WaitStepParameters.class);
    orchestrationElements.put("stageOverridesConfig", StageOverridesConfig.class);
    orchestrationElements.put("serviceSpec", ServiceSpec.class);
    orchestrationElements.put("cdPipeline", CDPipeline.class);
    orchestrationElements.put("pcfRouteUpdateRequestConfigData", PcfRouteUpdateRequestConfigData.class);
    orchestrationElements.put("pcfConfig", PcfConfig.class);
    orchestrationElements.put("stageVariables", StageVariables.class);
    orchestrationElements.put("k8sManifest", K8sManifest.class);
    orchestrationElements.put("deploymentState_deployment", DeploymentStage.Deployment.class);
    orchestrationElements.put("helmChartInfo", HelmChartInfo.class);
    orchestrationElements.put("infrastructureSpec", InfrastructureSpec.class);
    orchestrationElements.put("serviceOutcome_artifactsOutcome", ServiceOutcome.ArtifactsOutcome.class);
    orchestrationElements.put("ecsBGSetupData", EcsBGSetupData.class);
    orchestrationElements.put("deploymentStage", DeploymentStage.class);
    orchestrationElements.put("pcfAppSetupTimeDetails", PcfAppSetupTimeDetails.class);
    orchestrationElements.put("pcfCommandRequest", PcfCommandRequest.class);
    orchestrationElements.put("dockerHubArtifactConfig", DockerHubArtifactConfig.class);
    orchestrationElements.put("gcrArtifactConfig", GcrArtifactConfig.class);
    orchestrationElements.put("gitStore", GitStore.class);
    orchestrationElements.put("manifestConfig", ManifestConfig.class);
    orchestrationElements.put("valuesManifest", ValuesManifest.class);
    orchestrationElements.put("cdPhase", CDPhase.class);
    orchestrationElements.put("serviceUseFromStage", ServiceUseFromStage.class);
    orchestrationElements.put("serviceUseFromStage_overrides", Overrides.class);
    orchestrationElements.put("infraUseFromStage_overrides", InfraUseFromStage.Overrides.class);
    orchestrationElements.put("infraUseFromStage", InfraUseFromStage.class);
    orchestrationElements.put("environmentStepParameters", EnvironmentStepParameters.class);
    orchestrationElements.put("infraStepParameters", InfraStepParameters.class);
    orchestrationElements.put("manifestOverrideSets", ManifestOverrideSets.class);
    orchestrationElements.put("artifactOverrideSets", ArtifactOverrideSets.class);
    orchestrationElements.put("sidecarArtifact", SidecarArtifact.class);
  }
}
