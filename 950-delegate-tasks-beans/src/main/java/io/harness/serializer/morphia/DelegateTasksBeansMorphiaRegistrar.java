package io.harness.serializer.morphia;

import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.executioncapability.AlwaysFalseValidationCapability;
import io.harness.delegate.beans.executioncapability.AwsRegionCapability;
import io.harness.delegate.beans.executioncapability.ChartMuseumCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.HelmInstallationCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
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
import io.harness.delegate.task.artifacts.request.ArtifactTaskParameters;
import io.harness.delegate.task.artifacts.response.ArtifactTaskResponse;
import io.harness.delegate.task.gcp.request.GcpValidationRequest;
import io.harness.delegate.task.gcp.response.GcpValidationTaskResponse;
import io.harness.delegate.task.spotinst.request.SpotInstDeployTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class DelegateTasksBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(HDelegateTask.class);
    set.add(ExecutionCapabilityDemander.class);
    set.add(ExecutionCapability.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("delegate.beans.ErrorNotifyResponseData", ErrorNotifyResponseData.class);
    h.put("delegate.beans.executioncapability.AlwaysFalseValidationCapability", AlwaysFalseValidationCapability.class);
    h.put("delegate.beans.executioncapability.AwsRegionCapability", AwsRegionCapability.class);
    h.put("delegate.beans.executioncapability.ChartMuseumCapability", ChartMuseumCapability.class);
    h.put("delegate.beans.executioncapability.HelmInstallationCapability", HelmInstallationCapability.class);
    h.put("delegate.beans.executioncapability.HttpConnectionExecutionCapability",
        HttpConnectionExecutionCapability.class);
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
    h.put("delegate.task.gcp.request.GcpValidationRequest", GcpValidationRequest.class);
    h.put("delegate.task.gcp.response.GcpValidationTaskResponse", GcpValidationTaskResponse.class);
  }
}
