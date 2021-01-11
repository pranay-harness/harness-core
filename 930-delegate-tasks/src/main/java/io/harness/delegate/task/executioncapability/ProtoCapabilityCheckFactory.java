package io.harness.delegate.task.executioncapability;

import io.harness.capability.CapabilityParameters;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ProtoCapabilityCheckFactory {
  @Inject AwsRegionCapabilityCheck awsRegionCapabilityCheck;
  @Inject ChartMuseumCapabilityCheck chartMuseumCapabilityCheck;
  @Inject HelmInstallationCapabilityCheck helmInstallationCapabilityCheck;
  @Inject HttpConnectionExecutionCapabilityCheck httpConnectionExecutionCapabilityCheck;
  @Inject ProcessExecutorCapabilityCheck processExecutorCapabilityCheck;
  @Inject SftpCapabilityCheck sftpCapabilityCheck;
  @Inject SmbConnectionCapabilityCheck smbConnectionCapabilityCheck;
  @Inject SmtpCapabilityCheck smtpCapabilityCheck;
  @Inject SocketConnectivityCapabilityCheck socketConnectivityCapabilityCheck;
  @Inject SystemEnvCapabilityCheck systemEnvCapabilityCheck;

  public ProtoCapabilityCheck obtainCapabilityCheck(CapabilityParameters parameters) {
    switch (parameters.getCapabilityCase()) {
      case AWS_REGION_PARAMETERS:
        return awsRegionCapabilityCheck;
      case CHART_MUSEUM_PARAMETERS:
        return chartMuseumCapabilityCheck;
      case HELM_INSTALLATION_PARAMETERS:
        return helmInstallationCapabilityCheck;
      case HTTP_CONNECTION_PARAMETERS:
        return httpConnectionExecutionCapabilityCheck;
      case PROCESS_EXECUTOR_PARAMETERS:
        return processExecutorCapabilityCheck;
      case SFTP_CAPABILITY_PARAMETERS:
        return sftpCapabilityCheck;
      case SMB_CONNECTION_PARAMETERS:
        return smbConnectionCapabilityCheck;
      case SMTP_PARAMETERS:
        return smtpCapabilityCheck;
      case SOCKET_CONNECTIVITY_PARAMETERS:
        return socketConnectivityCapabilityCheck;
      case SYSTEM_ENV_PARAMETERS:
        return systemEnvCapabilityCheck;
      default:
        return null;
    }
  }
}
