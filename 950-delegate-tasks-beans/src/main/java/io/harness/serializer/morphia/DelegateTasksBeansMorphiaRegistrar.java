package io.harness.serializer.morphia;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.executioncapability.AlwaysFalseValidationCapability;
import io.harness.delegate.beans.executioncapability.AwsRegionCapability;
import io.harness.delegate.beans.executioncapability.ChartMuseumCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.GitInstallationCapability;
import io.harness.delegate.beans.executioncapability.HelmInstallationCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.KustomizeCapability;
import io.harness.delegate.beans.executioncapability.LiteEngineConnectionCapability;
import io.harness.delegate.beans.executioncapability.PcfAutoScalarCapability;
import io.harness.delegate.beans.executioncapability.PcfInstallationCapability;
import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.beans.executioncapability.SmbConnectionCapability;
import io.harness.delegate.beans.executioncapability.SmtpCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.task.HDelegateTask;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.docker.DockerArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.gcr.GcrArtifactDelegateRequest;
import io.harness.delegate.task.artifacts.gcr.GcrArtifactDelegateResponse;
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.delegate.task.gcp.request.GcpValidationRequest;
import io.harness.delegate.task.gcp.response.GcpValidationTaskResponse;
import io.harness.delegate.task.spotinst.request.SpotInstDeployTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.core.models.Secret;

import software.wings.helpers.ext.ecs.response.EcsServiceDeployResponse;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;

import java.util.Set;

@OwnedBy(HarnessTeam.DEL)
@BreakDependencyOn("io.harness.capability.CapabilityRequirement")
@BreakDependencyOn("io.harness.capability.CapabilitySubjectPermission")
public class DelegateTasksBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(CapabilityRequirement.class);
    set.add(CapabilitySubjectPermission.class);
    set.add(CapabilityTaskSelectionDetails.class);
    set.add(HDelegateTask.class);
    set.add(ExecutionCapabilityDemander.class);
    set.add(ExecutionCapability.class);
    set.add(Secret.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("delegate.beans.ErrorNotifyResponseData", ErrorNotifyResponseData.class);
    h.put("delegate.beans.executioncapability.AlwaysFalseValidationCapability", AlwaysFalseValidationCapability.class);
    h.put("delegate.beans.executioncapability.AwsRegionCapability", AwsRegionCapability.class);
    h.put("delegate.beans.executioncapability.ChartMuseumCapability", ChartMuseumCapability.class);
    h.put("delegate.beans.executioncapability.GitInstallationCapability", GitInstallationCapability.class);
    h.put("delegate.beans.executioncapability.HelmInstallationCapability", HelmInstallationCapability.class);
    h.put("delegate.beans.executioncapability.HttpConnectionExecutionCapability",
        HttpConnectionExecutionCapability.class);
    h.put("delegate.beans.executioncapability.LiteEngineConnectionCapability", LiteEngineConnectionCapability.class);
    h.put("delegate.beans.executioncapability.KustomizeCapability", KustomizeCapability.class);
    h.put("delegate.beans.executioncapability.PcfAutoScalarCapability", PcfAutoScalarCapability.class);
    h.put("delegate.beans.executioncapability.PcfInstallationCapability", PcfInstallationCapability.class);
    h.put("delegate.beans.executioncapability.ProcessExecutorCapability", ProcessExecutorCapability.class);
    h.put("delegate.beans.executioncapability.SmbConnectionCapability", SmbConnectionCapability.class);
    h.put("delegate.beans.executioncapability.SmtpCapability", SmtpCapability.class);
    h.put("delegate.beans.executioncapability.SocketConnectivityExecutionCapability",
        SocketConnectivityExecutionCapability.class);
    h.put("delegate.beans.executioncapability.SystemEnvCheckerCapability", SystemEnvCheckerCapability.class);
    h.put("delegate.beans.executioncapability.SelectorCapability", SelectorCapability.class);
    h.put("delegate.command.CommandExecutionResult", CommandExecutionResult.class);
    h.put("delegate.task.spotinst.request.SpotInstDeployTaskParameters", SpotInstDeployTaskParameters.class);
    h.put("delegate.task.spotinst.request.SpotInstSetupTaskParameters", SpotInstSetupTaskParameters.class);
    h.put("delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters", SpotInstSwapRoutesTaskParameters.class);
    h.put("waiter.ErrorNotifyResponseData", ErrorNotifyResponseData.class);
    h.put("delegate.task.artifacts.docker.DockerArtifactDelegateResponse", DockerArtifactDelegateResponse.class);
    h.put("delegate.task.artifacts.response.ArtifactTaskResponse", ArtifactTaskResponse.class);
    h.put("delegate.task.artifacts.request.ArtifactTaskParameters", ArtifactTaskParameters.class);
    h.put("delegate.task.artifacts.docker.DockerArtifactDelegateRequest", DockerArtifactDelegateRequest.class);
    h.put("delegate.task.artifacts.gcr.GcrArtifactDelegateRequest", GcrArtifactDelegateRequest.class);
    h.put("delegate.task.artifacts.gcr.GcrArtifactDelegateResponse", GcrArtifactDelegateResponse.class);
    h.put("delegate.task.gcp.request.GcpValidationRequest", GcpValidationRequest.class);
    h.put("delegate.task.gcp.response.GcpValidationTaskResponse", GcpValidationTaskResponse.class);
    h.put("software.wings.helpers.ext.ecs.response.EcsServiceDeployResponse", EcsServiceDeployResponse.class);

    w.put("helpers.ext.helm.response.HelmInstallCommandResponse", HelmInstallCommandResponse.class);
  }
}
