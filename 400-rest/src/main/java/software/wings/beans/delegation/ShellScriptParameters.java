package software.wings.beans.delegation;

import static io.harness.expression.Expression.ALLOW_SECRETS;
import static io.harness.expression.Expression.DISALLOW_SECRETS;

import static software.wings.common.Constants.HARNESS_KUBE_CONFIG_PATH;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.ActivityAccess;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.KerberosConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.core.local.executors.ShellExecutorConfig;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.delegatetasks.validation.capabilities.ShellConnectionCapability;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;
import software.wings.sm.states.ShellScriptState;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.apache.commons.lang3.StringUtils;

@Value
@Builder
public class ShellScriptParameters implements TaskParameters, ActivityAccess, ExecutionCapabilityDemander {
  public static final String CommandUnit = "Execute";

  private String accountId;
  private final String appId;
  private final String activityId;
  @Expression(DISALLOW_SECRETS) final String host;
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
  @Expression(ALLOW_SECRETS) @NonFinal @Setter String script;
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
  private final boolean localOverrideFeatureFlag;
  private final boolean saveExecutionLogs;
  boolean disableWinRMCommandEncodingFFSet; // DISABLE_WINRM_COMMAND_ENCODING

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
    encryptionService.decrypt(hostConnectionAttributes, keyEncryptedDataDetails, false);
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
        .withKerberosConfig(kerberosConfig)
        .withKey(hostConnectionAttributes.getKey())
        .withKeyPassphrase(hostConnectionAttributes.getPassphrase())
        .withSshPassword(hostConnectionAttributes.getSshPassword())
        .withPassword(hostConnectionAttributes.getKerberosPassword());
    return sshSessionConfigBuilder.build();
  }

  public WinRmSessionConfig winrmSessionConfig(EncryptionService encryptionService) throws IOException {
    encryptionService.decrypt(winrmConnectionAttributes, winrmConnectionEncryptedDataDetails, false);

    return WinRmSessionConfig.builder()
        .accountId(accountId)
        .appId(appId)
        .executionId(activityId)
        .commandUnitName(CommandUnit)
        .hostname(host)
        .authenticationScheme(winrmConnectionAttributes.getAuthenticationScheme())
        .domain(winrmConnectionAttributes.getDomain())
        .username(winrmConnectionAttributes.getUsername())
        .password(winrmConnectionAttributes.isUseKeyTab() ? StringUtils.EMPTY
                                                          : String.valueOf(winrmConnectionAttributes.getPassword()))
        .useKeyTab(winrmConnectionAttributes.isUseKeyTab())
        .keyTabFilePath(winrmConnectionAttributes.getKeyTabFilePath())
        .port(winrmConnectionAttributes.getPort())
        .useSSL(winrmConnectionAttributes.isUseSSL())
        .skipCertChecks(winrmConnectionAttributes.isSkipCertChecks())
        .workingDirectory(workingDirectory)
        .environment(getResolvedEnvironmentVariables())
        .useNoProfile(winrmConnectionAttributes.isUseNoProfile())
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
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>();

    if (executeOnDelegate) {
      if (containerServiceParams != null) {
        SettingAttribute settingAttribute = containerServiceParams.getSettingAttribute();
        if (settingAttribute != null) {
          SettingValue value = settingAttribute.getValue();
          boolean useKubernetesDelegate =
              value instanceof KubernetesClusterConfig && ((KubernetesClusterConfig) value).isUseKubernetesDelegate();
          boolean isKubernetes =
              value instanceof GcpConfig || value instanceof AzureConfig || value instanceof KubernetesClusterConfig;
          if (useKubernetesDelegate || (isKubernetes && script.contains(HARNESS_KUBE_CONFIG_PATH))) {
            return containerServiceParams.fetchRequiredExecutionCapabilities(maskingEvaluator);
          }
        }
      }

      if (scriptType == ScriptType.POWERSHELL) {
        executionCapabilities.add(ProcessExecutorCapabilityGenerator.buildProcessExecutorCapability(
            "DELEGATE_POWERSHELL", Arrays.asList("/bin/sh", "-c", "pwsh -Version")));
      }
      return executionCapabilities;
    }
    executionCapabilities.add(ShellConnectionCapability.builder().shellScriptParameters(this).build());
    return executionCapabilities;
  }
}
