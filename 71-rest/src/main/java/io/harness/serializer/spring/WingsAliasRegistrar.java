package io.harness.serializer.spring;

import io.harness.redesign.advisers.HttpResponseCodeSwitchAdviserParameters;
import io.harness.redesign.states.email.EmailStepParameters;
import io.harness.redesign.states.http.BasicHttpStepParameters;
import io.harness.redesign.states.http.chain.BasicHttpChainStepParameters;
import io.harness.redesign.states.shell.ShellScriptStepParameters;
import io.harness.redesign.states.shell.ShellScriptVariablesSweepingOutput;
import io.harness.redesign.states.wait.WaitStepParameters;
import io.harness.spring.AliasRegistrar;

import software.wings.api.AwsAmiInfoVariables;
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
    orchestrationElements.put("basicHttpChainStepParameters", BasicHttpChainStepParameters.class);
    orchestrationElements.put("basicHttpStepParameters", BasicHttpStepParameters.class);
    orchestrationElements.put("containerRollbackRequestElement", ContainerRollbackRequestElement.class);
    orchestrationElements.put("containerServiceElement", ContainerServiceElement.class);
    orchestrationElements.put("deploySweepingOutputPcf", DeploySweepingOutputPcf.class);
    orchestrationElements.put("emailStepParameters", EmailStepParameters.class);
    orchestrationElements.put("httpResponseCodeSwitchAdviserParameters", HttpResponseCodeSwitchAdviserParameters.class);
    orchestrationElements.put("httpStateExecutionData", HttpStateExecutionData.class);
    orchestrationElements.put("infoVariables", InfoVariables.class);
    orchestrationElements.put("infraMappingSweepingOutput", InfraMappingSweepingOutput.class);
    orchestrationElements.put("instanceInfoVariables", InstanceInfoVariables.class);
    orchestrationElements.put("k8sHelmDeploymentElement", K8sHelmDeploymentElement.class);
    orchestrationElements.put("phaseExecutionData", PhaseExecutionData.class);
    orchestrationElements.put("phaseExecutionSummary", PhaseExecutionSummary.class);
    orchestrationElements.put("scriptStateExecutionData", ScriptStateExecutionData.class);
    orchestrationElements.put("serviceArtifactElements", ServiceArtifactElements.class);
    orchestrationElements.put("serviceArtifactVariableElements", ServiceArtifactVariableElements.class);
    orchestrationElements.put("serviceInstanceIdsParam", ServiceInstanceIdsParam.class);
    orchestrationElements.put("setupSweepingOutputPcf", SetupSweepingOutputPcf.class);
    orchestrationElements.put("shellScriptStepParameters", ShellScriptStepParameters.class);
    orchestrationElements.put("shellScriptVariablesSweepingOutput", ShellScriptVariablesSweepingOutput.class);
    orchestrationElements.put("simpleWorkflowParam", SimpleWorkflowParam.class);
    orchestrationElements.put("swapRouteRollbackSweepingOutputPcf", SwapRouteRollbackSweepingOutputPcf.class);
    orchestrationElements.put("terraformApplyMarkerParam", TerraformApplyMarkerParam.class);
    orchestrationElements.put("waitStateExecutionData", WaitStateExecutionData.class);
    orchestrationElements.put("waitStepParameters", WaitStepParameters.class);
    orchestrationElements.put("pcfRouteUpdateRequestConfigData", PcfRouteUpdateRequestConfigData.class);
    orchestrationElements.put("pcfConfig", PcfConfig.class);
    orchestrationElements.put("helmChartInfo", HelmChartInfo.class);
    orchestrationElements.put("ecsBGSetupData", EcsBGSetupData.class);
    orchestrationElements.put("pcfAppSetupTimeDetails", PcfAppSetupTimeDetails.class);
    orchestrationElements.put("pcfCommandRequest", PcfCommandRequest.class);
    orchestrationElements.put("awsAmiInfoVariables", AwsAmiInfoVariables.class);
  }
}
