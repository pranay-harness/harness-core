package software.wings.beans.delegation;

import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import io.harness.delegate.task.protocol.TaskParameters;
import io.harness.expression.Expression;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import software.wings.api.ScriptType;
import software.wings.beans.HostConnectionAttributes;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class ShellScriptParameters implements TaskParameters {
  public static final String CommandUnit = "Execute";

  @Setter private String accountId;
  private final String appId;
  private final String activityId;
  private final String host;
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
    return aSshSessionConfig()
        .withAccountId(accountId)
        .withAppId(appId)
        .withExecutionId(activityId)
        .withHost(host)
        .withUserName(userName)
        .withKey(encryptionService.getDecryptedValue(
            fetchEncryptedDataDetail(keyEncryptedDataDetails, HostConnectionAttributes.KEY_KEY)))
        .withKeyPassphrase(encryptionService.getDecryptedValue(
            fetchEncryptedDataDetail(keyEncryptedDataDetails, HostConnectionAttributes.KEY_PASSPHRASE)))
        .withKeyPath(keyPath)
        .withKeyLess(keyless)
        .withWorkingDirectory(workingDirectory)
        .withCommandUnitName(CommandUnit)
        .withPort(port)
        .withKeyName(keyName)
        .withAccessType(accessType)
        .build();
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
        .build();
  }
}
