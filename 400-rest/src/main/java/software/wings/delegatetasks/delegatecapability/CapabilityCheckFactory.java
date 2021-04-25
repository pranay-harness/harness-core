package software.wings.delegatetasks.delegatecapability;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.task.executioncapability.AlwaysFalseValidationCapabilityCheck;
import io.harness.delegate.task.executioncapability.AwsRegionCapabilityCheck;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import io.harness.delegate.task.executioncapability.ChartMuseumCapabilityCheck;
import io.harness.delegate.task.executioncapability.GitConnectionNGCapabilityChecker;
import io.harness.delegate.task.executioncapability.HelmInstallationCapabilityCheck;
import io.harness.delegate.task.executioncapability.HttpConnectionExecutionCapabilityCheck;
import io.harness.delegate.task.executioncapability.KustomizeCapabilityCheck;
import io.harness.delegate.task.executioncapability.LiteEngineConnectionCapabilityCheck;
import io.harness.delegate.task.executioncapability.PcfAutoScalarCapabilityCheck;
import io.harness.delegate.task.executioncapability.PcfConnectivityCapabilityCheck;
import io.harness.delegate.task.executioncapability.PcfInstallationCapabilityCheck;
import io.harness.delegate.task.executioncapability.ProcessExecutorCapabilityCheck;
import io.harness.delegate.task.executioncapability.SftpCapabilityCheck;
import io.harness.delegate.task.executioncapability.SmbConnectionCapabilityCheck;
import io.harness.delegate.task.executioncapability.SmtpCapabilityCheck;
import io.harness.delegate.task.executioncapability.SocketConnectivityCapabilityCheck;
import io.harness.delegate.task.executioncapability.SystemEnvCapabilityCheck;

import software.wings.delegatetasks.validation.capabilitycheck.ClusterMasterUrlCapabilityCheck;
import software.wings.delegatetasks.validation.capabilitycheck.GitConnectionCapabilityCheck;
import software.wings.delegatetasks.validation.capabilitycheck.HelmCommandCapabilityCheck;
import software.wings.delegatetasks.validation.capabilitycheck.SSHHostValidationCapabilityCheck;
import software.wings.delegatetasks.validation.capabilitycheck.ShellConnectionCapabilityCheck;
import software.wings.delegatetasks.validation.capabilitycheck.WinrmHostValidationCapabilityCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class CapabilityCheckFactory {
  @Inject SocketConnectivityCapabilityCheck socketConnectivityCapabilityCheck;
  @Inject ProcessExecutorCapabilityCheck processExecutorCapabilityCheck;
  @Inject AwsRegionCapabilityCheck awsRegionCapabilityCheck;
  @Inject SystemEnvCapabilityCheck systemEnvCapabilityCheck;
  @Inject HttpConnectionExecutionCapabilityCheck httpConnectionExecutionCapabilityCheck;
  @Inject LiteEngineConnectionCapabilityCheck liteEngineConnectionCapabilityCheck;
  @Inject SmtpCapabilityCheck smtpCapabilityCheck;
  @Inject AlwaysFalseValidationCapabilityCheck alwaysFalseValidationCapabilityCheck;
  @Inject WinrmHostValidationCapabilityCheck winrmHostValidationCapabilityCheck;
  @Inject SSHHostValidationCapabilityCheck sshHostValidationCapabilityCheck;
  @Inject SftpCapabilityCheck sftpCapabilityCheck;
  @Inject PcfConnectivityCapabilityCheck pcfConnectivityCapabilityCheck;
  @Inject PcfAutoScalarCapabilityCheck pcfAutoScalarCapabilityCheck;
  @Inject PcfInstallationCapabilityCheck pcInstallationCapabilityCheck;
  @Inject HelmCommandCapabilityCheck helmCommandCapabilityCheck;
  @Inject HelmInstallationCapabilityCheck helmInstallationCapabilityCheck;
  @Inject ChartMuseumCapabilityCheck chartMuseumCapabilityCheck;
  @Inject ClusterMasterUrlCapabilityCheck clusterMasterUrlCapabilityCheck;
  @Inject ShellConnectionCapabilityCheck shellConnectionCapabilityCheck;
  @Inject GitConnectionCapabilityCheck gitConnectionCapabilityCheck;
  @Inject KustomizeCapabilityCheck kustomizeCapabilityCheck;
  @Inject SmbConnectionCapabilityCheck smbConnectionCapabilityCheck;
  @Inject GitConnectionNGCapabilityChecker gitConnectionNGCapabilityCheck;

  public CapabilityCheck obtainCapabilityCheck(CapabilityType capabilityCheckType) {
    switch (capabilityCheckType) {
      case SOCKET:
        return socketConnectivityCapabilityCheck;
      case PROCESS_EXECUTOR:
        return processExecutorCapabilityCheck;
      case AWS_REGION:
        return awsRegionCapabilityCheck;
      case SYSTEM_ENV:
        return systemEnvCapabilityCheck;
      case HTTP:
        return httpConnectionExecutionCapabilityCheck;
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
      case PCF_CONNECTIVITY:
        return pcfConnectivityCapabilityCheck;
      case PCF_AUTO_SCALAR:
        return pcfAutoScalarCapabilityCheck;
      case PCF_INSTALL:
        return pcInstallationCapabilityCheck;
      case HELM_COMMAND:
        return helmCommandCapabilityCheck;
      case HELM_INSTALL:
        return helmInstallationCapabilityCheck;
      case CHART_MUSEUM:
        return chartMuseumCapabilityCheck;
      case CLUSTER_MASTER_URL:
        return clusterMasterUrlCapabilityCheck;
      case SHELL_CONNECTION:
        return shellConnectionCapabilityCheck;
      case GIT_CONNECTION:
        return gitConnectionCapabilityCheck;
      case KUSTOMIZE:
        return kustomizeCapabilityCheck;
      case SMB:
        return smbConnectionCapabilityCheck;
      case GIT_CONNECTION_NG:
        return gitConnectionNGCapabilityCheck;
      case LITE_ENGINE:
        return liteEngineConnectionCapabilityCheck;
      default:
        return null;
    }
  }
}
