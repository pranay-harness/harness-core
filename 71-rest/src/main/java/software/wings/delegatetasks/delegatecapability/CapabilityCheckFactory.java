package software.wings.delegatetasks.delegatecapability;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.task.executioncapability.AlwaysFalseValidationCapabilityCheck;
import io.harness.delegate.task.executioncapability.AwsRegionCapabilityCheck;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import io.harness.delegate.task.executioncapability.ChartMuseumCapabilityCheck;
import io.harness.delegate.task.executioncapability.HelmCapabilityCheck;
import io.harness.delegate.task.executioncapability.HttpConnectionExecutionCapabilityCheck;
import io.harness.delegate.task.executioncapability.IgnoreValidationCapabilityCheck;
import io.harness.delegate.task.executioncapability.ProcessExecutorCapabilityCheck;
import io.harness.delegate.task.executioncapability.SocketConnectivityCapabilityCheck;
import io.harness.delegate.task.executioncapability.SystemEnvCapabilityCheck;
import software.wings.delegatetasks.validation.capabilitycheck.SSHHostValidationCapabilityCheck;
import software.wings.delegatetasks.validation.capabilitycheck.SftpCapabilityCheck;
import software.wings.delegatetasks.validation.capabilitycheck.SmtpCapabilityCheck;
import software.wings.delegatetasks.validation.capabilitycheck.WinrmHostValidationCapabilityCheck;

@Singleton
public class CapabilityCheckFactory {
  @Inject SocketConnectivityCapabilityCheck socketConnectivityCapabilityCheck;
  @Inject IgnoreValidationCapabilityCheck ignoreValidationCapabilityCheck;
  @Inject ProcessExecutorCapabilityCheck processExecutorCapabilityCheck;
  @Inject AwsRegionCapabilityCheck awsRegionCapabilityCheck;
  @Inject SystemEnvCapabilityCheck systemEnvCapabilityCheck;
  @Inject HttpConnectionExecutionCapabilityCheck httpConnectionExecutionCapabilityCheck;
  @Inject HelmCapabilityCheck helmCapabilityCheck;
  @Inject ChartMuseumCapabilityCheck chartMuseumCapabilityCheck;
  @Inject SmtpCapabilityCheck smtpCapabilityCheck;
  @Inject AlwaysFalseValidationCapabilityCheck alwaysFalseValidationCapabilityCheck;
  @Inject WinrmHostValidationCapabilityCheck winrmHostValidationCapabilityCheck;
  @Inject SSHHostValidationCapabilityCheck sshHostValidationCapabilityCheck;
  @Inject SftpCapabilityCheck sftpCapabilityCheck;

  public CapabilityCheck obtainCapabilityCheck(CapabilityType capabilityCheckType) {
    switch (capabilityCheckType) {
      case SOCKET:
        return socketConnectivityCapabilityCheck;
      case ALWAYS_TRUE:
        return ignoreValidationCapabilityCheck;
      case PROCESS_EXECUTOR:
        return processExecutorCapabilityCheck;
      case AWS_REGION:
        return awsRegionCapabilityCheck;
      case SYSTEM_ENV:
        return systemEnvCapabilityCheck;
      case HTTP:
        return httpConnectionExecutionCapabilityCheck;
      case HELM:
        return helmCapabilityCheck;
      case CHART_MUSEUM:
        return chartMuseumCapabilityCheck;
      case SMTP:
        return smtpCapabilityCheck;
      case ALWAYS_FALSE:
        return alwaysFalseValidationCapabilityCheck;
      case WINRM_HOST_CONNECTION:
        return winrmHostValidationCapabilityCheck;
      case SSH_HOST_CONNECTION:
        return sshHostValidationCapabilityCheck;
      case SFTP:
        return sftpCapabilityCheck;
      default:
        return null;
    }
  }
}
