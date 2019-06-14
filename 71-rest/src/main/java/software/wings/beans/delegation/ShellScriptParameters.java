package software.wings.beans.delegation;

import static io.harness.govern.Switch.unhandled;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.SSHConnectionExecutionCapabilityGenerator;
import io.harness.delegate.task.mixin.WinRMExecutionCapabilityGenerator;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.expression.Expression;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.KerberosConfig;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.core.local.executors.ShellExecutorConfig;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.ShellScriptState;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class ShellScriptParameters implements TaskParameters, ExecutionCapabilityDemander {
  public static final String CommandUnit = "Execute";

  private String accountId;
  private final String appId;
  private final String activityId;
  @Expression final String host;
  private final String userName;
  private final ShellScriptState.ConnectionType connectionType;
  private final List<EncryptedDataDetail> keyEncryptedDataDetails;
  private final WinRmConnectionAttributes winrmConnectionAttributes;
  private final List<EncryptedDataDetail> winrmConnectionEncryptedDataDetails;
  private final ContainerServiceParams containerServiceParams;
  private final Map<String, String> serviceVariables;
  private final Map<String, String> safeDisplayServiceVariables;
  private final Map<String, String> environment;
  private final String workingDirectory;
  private final ScriptType scriptType;
  @Expression private final String script;
  private final boolean executeOnDelegate;
  private final String outputVars;
  private final HostConnectionAttributes hostConnectionAttributes;
  private final String keyPath;
  private final boolean keyless;
  private final Integer port;
  private final HostConnectionAttributes.AccessType accessType;
  private final HostConnectionAttributes.AuthenticationScheme authenticationScheme;
  private final KerberosConfig kerberosConfig;
  private final String keyName;

  private Map<String, String> getResolvedEnvironmentVariables() {
    Map<String, String> resolvedEnvironment = new HashMap<>();

    if (environment != null) {
      resolvedEnvironment.putAll(environment);
    }

    if (serviceVariables != null) {
      resolvedEnvironment.putAll(serviceVariables);
    }

    return resolvedEnvironment;
  }

  public SshSessionConfig sshSessionConfig(EncryptionService encryptionService) throws IOException {
    encryptionService.decrypt(hostConnectionAttributes, keyEncryptedDataDetails);
    SshSessionConfig.Builder sshSessionConfigBuilder = aSshSessionConfig();
    sshSessionConfigBuilder.withAccountId(accountId)
        .withAppId(appId)
        .withExecutionId(activityId)
        .withHost(host)
        .withUserName(userName)
        .withKeyPath(keyPath)
        .withKeyLess(keyless)
        .withWorkingDirectory(workingDirectory)
        .withCommandUnitName(CommandUnit)
        .withPort(port)
        .withKeyName(keyName)
        .withAccessType(accessType)
        .withAuthenticationScheme(authenticationScheme)
        .withKerberosConfig(kerberosConfig);
    EncryptedDataDetail encryptedDataDetailKey =
        fetchEncryptedDataDetail(keyEncryptedDataDetails, HostConnectionAttributes.KEY_KEY);
    if (encryptedDataDetailKey != null) {
      sshSessionConfigBuilder.withKey(encryptionService.getDecryptedValue(encryptedDataDetailKey));
    }
    EncryptedDataDetail encryptedDataDetailPassPhrase =
        fetchEncryptedDataDetail(keyEncryptedDataDetails, HostConnectionAttributes.KEY_PASSPHRASE);
    if (encryptedDataDetailPassPhrase != null) {
      sshSessionConfigBuilder.withKeyPassphrase(encryptionService.getDecryptedValue(encryptedDataDetailPassPhrase));
    }
    EncryptedDataDetail encryptedSshPassword =
        fetchEncryptedDataDetail(keyEncryptedDataDetails, HostConnectionAttributes.KEY_SSH_PASSWORD);
    if (encryptedSshPassword != null) {
      sshSessionConfigBuilder.withSshPassword(encryptionService.getDecryptedValue(encryptedSshPassword));
    }
    EncryptedDataDetail encryptedKerberosPassword =
        fetchEncryptedDataDetail(keyEncryptedDataDetails, HostConnectionAttributes.KEY_KERBEROS_PASSWORD);
    if (encryptedKerberosPassword != null) {
      sshSessionConfigBuilder.withPassword(encryptionService.getDecryptedValue(encryptedKerberosPassword));
    }
    return sshSessionConfigBuilder.build();
  }

  private EncryptedDataDetail fetchEncryptedDataDetail(List<EncryptedDataDetail> encryptedDataDetails, String key) {
    return encryptedDataDetails.stream()
        .filter(encryptedDataDetail -> encryptedDataDetail.getFieldName().equals(key))
        .findFirst()
        .orElse(null);
  }

  public WinRmSessionConfig winrmSessionConfig(EncryptionService encryptionService) throws IOException {
    encryptionService.decrypt(winrmConnectionAttributes, winrmConnectionEncryptedDataDetails);
    return WinRmSessionConfig.builder()
        .accountId(accountId)
        .appId(appId)
        .executionId(activityId)
        .commandUnitName(CommandUnit)
        .hostname(host)
        .authenticationScheme(winrmConnectionAttributes.getAuthenticationScheme())
        .domain(winrmConnectionAttributes.getDomain())
        .username(winrmConnectionAttributes.getUsername())
        .password(String.valueOf(winrmConnectionAttributes.getPassword()))
        .port(winrmConnectionAttributes.getPort())
        .useSSL(winrmConnectionAttributes.isUseSSL())
        .skipCertChecks(winrmConnectionAttributes.isSkipCertChecks())
        .workingDirectory(workingDirectory)
        .environment(getResolvedEnvironmentVariables())
        .build();
  }

  public ShellExecutorConfig processExecutorConfig(
      ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper) {
    String kubeConfigContent = (containerServiceParams != null) && containerServiceParams.isKubernetesClusterConfig()
        ? containerDeploymentDelegateHelper.getKubeConfigFileContent(containerServiceParams)
        : "";
    return ShellExecutorConfig.builder()
        .accountId(accountId)
        .appId(appId)
        .executionId(activityId)
        .commandUnitName(CommandUnit)
        .workingDirectory(workingDirectory)
        .environment(getResolvedEnvironmentVariables())
        .kubeConfigContent(kubeConfigContent)
        .scriptType(scriptType)
        .build();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    switch (connectionType) {
      case SSH:
        return Arrays.asList(SSHConnectionExecutionCapabilityGenerator.buildSSHConnectionExecutionCapability(host));
      case WINRM:
        return Arrays.asList(WinRMExecutionCapabilityGenerator.buildWinRMExecutionCapability(
            host, Integer.toString(winrmConnectionAttributes.getPort())));
      default:
        unhandled(connectionType);
        return null;
    }
  }
}
