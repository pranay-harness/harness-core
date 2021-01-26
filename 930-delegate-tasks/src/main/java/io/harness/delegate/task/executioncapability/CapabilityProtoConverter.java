package io.harness.delegate.task.executioncapability;

import io.harness.capability.AwsRegionParameters;
import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.ChartMuseumParameters;
import io.harness.capability.GitInstallationParameters;
import io.harness.capability.HelmInstallationParameters;
import io.harness.capability.HttpConnectionParameters;
import io.harness.capability.ProcessExecutorParameters;
import io.harness.capability.SftpCapabilityParameters;
import io.harness.capability.SmbConnectionParameters;
import io.harness.capability.SocketConnectivityParameters;
import io.harness.capability.SystemEnvParameters;
import io.harness.delegate.beans.executioncapability.AwsRegionCapability;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HelmInstallationCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;
import io.harness.delegate.beans.executioncapability.SftpCapability;
import io.harness.delegate.beans.executioncapability.SmbConnectionCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.k8s.model.HelmVersion;

public class CapabilityProtoConverter {
  public static boolean shouldCompareResults(CapabilityParameters parameters) {
    if (parameters == null) {
      return false;
    }
    switch (parameters.getCapabilityCase()) {
      case AWS_REGION_PARAMETERS:
      case CHART_MUSEUM_PARAMETERS:
      case GIT_INSTALLATION_PARAMETERS:
      case HELM_INSTALLATION_PARAMETERS:
      case HTTP_CONNECTION_PARAMETERS:
      case PROCESS_EXECUTOR_PARAMETERS:
      case SFTP_CAPABILITY_PARAMETERS:
      case SMB_CONNECTION_PARAMETERS:
      case SOCKET_CONNECTIVITY_PARAMETERS:
      case SYSTEM_ENV_PARAMETERS:
        return true;
      case SMTP_PARAMETERS:
      default:
        return false;
    }
  }

  public static CapabilityParameters toProto(ExecutionCapability executionCapability) {
    CapabilityParameters.Builder builder = CapabilityParameters.newBuilder();
    switch (executionCapability.getCapabilityType()) {
      case AWS_REGION:
        AwsRegionCapability capability = (AwsRegionCapability) executionCapability;
        return builder.setAwsRegionParameters(AwsRegionParameters.newBuilder().setRegion(capability.getRegion()))
            .build();
      case CHART_MUSEUM:
        return builder.setChartMuseumParameters(ChartMuseumParameters.getDefaultInstance()).build();
      case GIT_INSTALLATION:
        return builder.setGitInstallationParameters(GitInstallationParameters.getDefaultInstance()).build();
      case HELM_INSTALL:
        HelmInstallationCapability helmInstallationCapability = (HelmInstallationCapability) executionCapability;
        return builder
            .setHelmInstallationParameters(HelmInstallationParameters.newBuilder().setHelmVersion(
                helmInstallationCapability.getVersion() == HelmVersion.V3 ? HelmInstallationParameters.HelmVersion.V3
                                                                          : HelmInstallationParameters.HelmVersion.V2))
            .build();
      case HTTP:
        HttpConnectionExecutionCapability httpConnectionExecutionCapability =
            (HttpConnectionExecutionCapability) executionCapability;
        return builder
            .setHttpConnectionParameters(
                HttpConnectionParameters.newBuilder().setUrl(httpConnectionExecutionCapability.fetchCapabilityBasis()))
            .build();
      case PROCESS_EXECUTOR:
        ProcessExecutorCapability processExecutorCapability = (ProcessExecutorCapability) executionCapability;
        return builder
            .setProcessExecutorParameters(ProcessExecutorParameters.newBuilder().addAllArgs(
                processExecutorCapability.getProcessExecutorArguments()))
            .build();
      case SFTP:
        SftpCapability sftpCapability = (SftpCapability) executionCapability;
        return builder
            .setSftpCapabilityParameters(SftpCapabilityParameters.newBuilder().setSftpUrl(sftpCapability.getSftpUrl()))
            .build();
      case SOCKET:
        SocketConnectivityExecutionCapability socketConnectivityExecutionCapability =
            (SocketConnectivityExecutionCapability) executionCapability;
        return builder
            .setSocketConnectivityParameters(
                SocketConnectivityParameters.newBuilder()
                    .setHostName(socketConnectivityExecutionCapability.getHostName())
                    .setPort(Integer.parseInt(socketConnectivityExecutionCapability.getPort()))
                    .setUrl(socketConnectivityExecutionCapability.getUrl()))
            .build();
      case SYSTEM_ENV:
        SystemEnvCheckerCapability systemEnvCheckerCapability = (SystemEnvCheckerCapability) executionCapability;
        return builder
            .setSystemEnvParameters(SystemEnvParameters.newBuilder()
                                        .setProperty(systemEnvCheckerCapability.getSystemPropertyName())
                                        .setComparate(systemEnvCheckerCapability.getComparate()))
            .build();
      case SMB:
        SmbConnectionCapability smbConnectionCapability = (SmbConnectionCapability) executionCapability;
        return builder
            .setSmbConnectionParameters(
                SmbConnectionParameters.newBuilder().setSmbUrl(smbConnectionCapability.getSmbUrl()))
            .build();
      default:
        return null;
    }
  }

  public static boolean hasDivergingResults(
      CapabilityResponse capabilityResponse, CapabilitySubjectPermission capabilitySubjectPermission) {
    return capabilityResponse.isValidated()
        ^ (capabilitySubjectPermission.getPermissionResult() == CapabilitySubjectPermission.PermissionResult.ALLOWED);
  }
}
