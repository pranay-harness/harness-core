package io.harness.redesign.states.shell;

import static com.google.common.collect.Lists.newArrayList;
import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.ListUtils.trimStrings;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static software.wings.beans.FeatureName.LOCAL_DELEGATE_CONFIG_OVERRIDE;
import static software.wings.beans.delegation.ShellScriptParameters.CommandUnit;
import static software.wings.sm.StateType.SHELL_SCRIPT;

import com.google.inject.Inject;

import io.harness.ambiance.Ambiance;
import io.harness.annotations.Redesign;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.data.algorithm.HashGenerator;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.facilitator.modes.task.AsyncTaskExecutable;
import io.harness.resolver.sweepingoutput.ExecutionSweepingOutputResolver;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.state.State;
import io.harness.state.StateType;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateResponse;
import io.harness.state.io.StateResponse.StateResponseBuilder;
import io.harness.state.io.StateTransput;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.ScriptStateExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.KerberosConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.command.Command.Builder;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ShellExecutionData;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.beans.delegation.ShellScriptParameters.ShellScriptParametersBuilder;
import software.wings.exception.ShellScriptException;
import software.wings.service.impl.ActivityHelperService;
import software.wings.service.impl.servicetemplates.ServiceTemplateHelper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
@Redesign
@Slf4j
public class ShellScriptState implements State, AsyncTaskExecutable {
  public static final StateType STATE_TYPE = StateType.builder().type(SHELL_SCRIPT.name()).build();

  @Inject private ActivityService activityService;
  @Inject private ActivityHelperService activityHelperService;
  @Inject private SettingsService settingsService;
  @Inject private SecretManager secretManager;
  @Inject private ServiceTemplateHelper serviceTemplateHelper;
  @Inject private ExecutionSweepingOutputResolver executionSweepingOutputResolver;
  @Inject private FeatureFlagService featureFlagService;

  @Override
  public StateType getType() {
    return STATE_TYPE;
  }

  @Override
  public DelegateTask obtainTask(Ambiance ambiance, StateParameters parameters, List<StateTransput> inputs) {
    ShellScriptStateParameters shellScriptStateParameters = (ShellScriptStateParameters) parameters;
    String activityId = createActivity(ambiance);

    Environment environment =
        (Environment) inputs.stream().filter(input -> input instanceof Environment).findFirst().orElse(null);
    InfrastructureMapping infrastructureMapping = (InfrastructureMapping) inputs.stream()
                                                      .filter(input -> input instanceof InfrastructureMapping)
                                                      .findFirst()
                                                      .orElse(null);
    String serviceTemplateId =
        infrastructureMapping == null ? null : serviceTemplateHelper.fetchServiceTemplateId(infrastructureMapping);

    String username = null;
    String keyPath = null;
    boolean keyless = false;
    Integer port = null;
    HostConnectionAttributes.AccessType accessType = null;
    HostConnectionAttributes.AuthenticationScheme authenticationScheme = null;
    String keyName = null;
    WinRmConnectionAttributes winRmConnectionAttributes = null;
    List<EncryptedDataDetail> winrmEdd = emptyList();
    List<EncryptedDataDetail> keyEncryptionDetails = emptyList();
    KerberosConfig kerberosConfig = null;

    HostConnectionAttributes hostConnectionAttributes = null;

    software.wings.sm.states.ShellScriptState.ConnectionType connectionType =
        shellScriptStateParameters.getConnectionType();
    if (connectionType == null) {
      connectionType = software.wings.sm.states.ShellScriptState.ConnectionType.SSH;
    }

    ScriptType scriptType = shellScriptStateParameters.getScriptType();
    if (scriptType == null) {
      scriptType = ScriptType.BASH;
    }

    if (!shellScriptStateParameters.isExecuteOnDelegate()) {
      if (connectionType == software.wings.sm.states.ShellScriptState.ConnectionType.SSH) {
        String sshKeyRef = shellScriptStateParameters.getSshKeyRef();
        if (isEmpty(sshKeyRef)) {
          throw new ShellScriptException("Valid SSH Connection Attribute not provided in Shell Script Step",
              ErrorCode.SSH_CONNECTION_ERROR, Level.ERROR, WingsException.USER);
        }

        SettingAttribute keySettingAttribute = settingsService.get(sshKeyRef);
        if (keySettingAttribute == null) {
          keySettingAttribute = settingsService.getSettingAttributeByName(getAccountId(ambiance), sshKeyRef);
        }

        if (keySettingAttribute == null) {
          throw new ShellScriptException("SSH Connection Attribute provided in Shell Script Step not found",
              ErrorCode.SSH_CONNECTION_ERROR, Level.ERROR, WingsException.USER);
        }
        hostConnectionAttributes = (HostConnectionAttributes) keySettingAttribute.getValue();
        username = ((HostConnectionAttributes) keySettingAttribute.getValue()).getUserName();
        keyPath = ((HostConnectionAttributes) keySettingAttribute.getValue()).getKeyPath();
        keyless = ((HostConnectionAttributes) keySettingAttribute.getValue()).isKeyless();
        port = ((HostConnectionAttributes) keySettingAttribute.getValue()).getSshPort();
        if (port == null) {
          port = 22;
        }
        accessType = ((HostConnectionAttributes) keySettingAttribute.getValue()).getAccessType();
        authenticationScheme = ((HostConnectionAttributes) keySettingAttribute.getValue()).getAuthenticationScheme();
        kerberosConfig = hostConnectionAttributes != null ? hostConnectionAttributes.getKerberosConfig() : null;
        keyName = keySettingAttribute.getUuid();
        keyEncryptionDetails = secretManager.getEncryptionDetails(
            (EncryptableSetting) keySettingAttribute.getValue(), getAppId(ambiance), ambiance.getPlanExecutionId());

      } else if (connectionType == software.wings.sm.states.ShellScriptState.ConnectionType.WINRM) {
        winRmConnectionAttributes =
            setupWinrmCredentials(ambiance, shellScriptStateParameters.getConnectionAttributes());
        username = winRmConnectionAttributes.getUsername();
        winrmEdd = secretManager.getEncryptionDetails(
            winRmConnectionAttributes, getAppId(ambiance), ambiance.getPlanExecutionId());
      }
    }

    String commandPath = shellScriptStateParameters.getCommandPath();
    if (StringUtils.isEmpty(commandPath)) {
      if (scriptType == ScriptType.BASH) {
        commandPath = "/tmp";
      } else if (scriptType == ScriptType.POWERSHELL) {
        commandPath = "%TEMP%";
        if (shellScriptStateParameters.isExecuteOnDelegate()) {
          commandPath = "/tmp";
        }
      }
    }

    List<String> tags = shellScriptStateParameters.getTags();
    List<String> allTags = newArrayList();
    List<String> renderedTags = newArrayList();
    if (isNotEmpty(tags)) {
      allTags.addAll(tags);
    }
    if (isNotEmpty(allTags)) {
      renderedTags = trimStrings(renderedTags);
    }

    ShellScriptParametersBuilder shellScriptParameters =
        ShellScriptParameters.builder()
            .accountId(getAccountId(ambiance))
            .appId(getAppId(ambiance))
            .activityId(activityId)
            .host(shellScriptStateParameters.getHost())
            .connectionType(connectionType)
            .winrmConnectionAttributes(winRmConnectionAttributes)
            .winrmConnectionEncryptedDataDetails(winrmEdd)
            .userName(username)
            .keyEncryptedDataDetails(keyEncryptionDetails)
            .workingDirectory(commandPath)
            .scriptType(scriptType)
            .script(shellScriptStateParameters.getScriptString())
            .executeOnDelegate(shellScriptStateParameters.isExecuteOnDelegate())
            .outputVars(shellScriptStateParameters.getOutputVars())
            .hostConnectionAttributes(hostConnectionAttributes)
            .keyless(keyless)
            .keyPath(keyPath)
            .port(port)
            .accessType(accessType)
            .authenticationScheme(authenticationScheme)
            .kerberosConfig(kerberosConfig)
            .localOverrideFeatureFlag(
                featureFlagService.isEnabled(LOCAL_DELEGATE_CONFIG_OVERRIDE, getAccountId(ambiance)))
            .keyName(keyName);

    int expressionFunctorToken = HashGenerator.generateIntegerHash();
    return DelegateTask.builder()
        .accountId(getAccountId(ambiance))
        .waitId(activityId)
        .tags(renderedTags)
        .appId(getAppId(ambiance))
        .data(TaskData.builder()
                  .async(true)
                  .taskType(TaskType.SCRIPT.name())
                  .parameters(new Object[] {shellScriptParameters.build()})
                  .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                  .expressionFunctorToken(expressionFunctorToken)
                  .build())
        .envId(environment == null ? null : environment.getUuid())
        .infrastructureMappingId(infrastructureMapping == null ? null : infrastructureMapping.getUuid())
        .serviceTemplateId(serviceTemplateId)
        .build();
  }

  @Override
  public StateResponse handleTaskResult(
      Ambiance ambiance, StateParameters parameters, Map<String, ResponseData> response) {
    ShellScriptStateParameters shellScriptStateParameters = (ShellScriptStateParameters) parameters;
    StateResponseBuilder stateResponseBuilder = StateResponse.builder();
    String activityId = response.keySet().iterator().next();
    ResponseData data = response.values().iterator().next();
    boolean saveSweepingOutputToContext = false;
    ExecutionStatus executionStatus = null;
    if (data instanceof CommandExecutionResult) {
      CommandExecutionResult commandExecutionResult = (CommandExecutionResult) data;

      switch (commandExecutionResult.getStatus()) {
        case SUCCESS:
          executionStatus = ExecutionStatus.SUCCESS;
          stateResponseBuilder.status(NodeExecutionStatus.SUCCEEDED);
          break;
        case FAILURE:
          executionStatus = ExecutionStatus.FAILED;
          stateResponseBuilder.status(NodeExecutionStatus.FAILED);
          break;
        case RUNNING:
          executionStatus = ExecutionStatus.RUNNING;
          stateResponseBuilder.status(NodeExecutionStatus.RUNNING);
          break;
        case QUEUED:
          executionStatus = ExecutionStatus.QUEUED;
          stateResponseBuilder.status(NodeExecutionStatus.QUEUED);
          break;
        default:
          throw new ShellScriptException(
              "Unhandled type CommandExecutionStatus: " + commandExecutionResult.getStatus().name(),
              ErrorCode.SSH_CONNECTION_ERROR, Level.ERROR, WingsException.USER);
      }

      stateResponseBuilder.failureInfo(
          StateResponse.FailureInfo.builder().errorMessage(commandExecutionResult.getErrorMessage()).build());

      ScriptStateExecutionData scriptStateExecutionData =
          ScriptStateExecutionData.builder().activityId(activityId).build();
      if (commandExecutionResult.getStatus() == SUCCESS) {
        Map<String, String> sweepingOutputEnvVariables =
            ((ShellExecutionData) ((CommandExecutionResult) data).getCommandExecutionData())
                .getSweepingOutputEnvVariables();
        scriptStateExecutionData.setSweepingOutputEnvVariables(sweepingOutputEnvVariables);
        saveSweepingOutputToContext = true;
      }
      stateResponseBuilder.outcome("data", scriptStateExecutionData);
    } else if (data instanceof ErrorNotifyResponseData) {
      stateResponseBuilder.status(NodeExecutionStatus.FAILED);
      stateResponseBuilder.failureInfo(
          StateResponse.FailureInfo.builder().errorMessage(((ErrorNotifyResponseData) data).getErrorMessage()).build());
      return stateResponseBuilder.build();
    } else {
      logger.error("Unhandled ResponseData class " + data.getClass().getCanonicalName(), new Exception(""));
    }

    StateResponse stateResponse = stateResponseBuilder.build();

    updateActivityStatus(activityId, getAppId(ambiance), executionStatus);

    if (saveSweepingOutputToContext) {
      executionSweepingOutputResolver.save(ambiance, shellScriptStateParameters.getSweepingOutputName(),
          ShellScriptVariablesSweepingOutput.builder()
              .variables(((ShellExecutionData) ((CommandExecutionResult) data).getCommandExecutionData())
                             .getSweepingOutputEnvVariables())
              .build(),
          0);
    }

    return stateResponse;
  }

  private String getAccountId(Ambiance ambiance) {
    return ambiance.getSetupAbstractions().get("accountId");
  }

  private String getAppId(Ambiance ambiance) {
    return ambiance.getSetupAbstractions().get("appId");
  }

  private String createActivity(Ambiance ambiance) {
    String appId = ambiance.getSetupAbstractions().get("appId");
    return activityService
        .save(Activity.builder()
                  .appId(appId)
                  .applicationName("appName")
                  .environmentId("envId")
                  .environmentName("envName")
                  .environmentType(EnvironmentType.PROD)
                  .workflowExecutionId(ambiance.getPlanExecutionId())
                  .workflowId(ambiance.getPlanExecutionId())
                  .workflowExecutionName(ambiance.getPlanExecutionId())
                  .workflowType(WorkflowType.PIPELINE)
                  .stateExecutionInstanceId("stateExecutionInstanceId")
                  .stateExecutionInstanceName("stateExecutionInstanceName")
                  .commandType(getType().getType())
                  .commandName(CommandUnit)
                  .commandUnits(singletonList(
                      Builder.aCommand().withName(CommandUnit).withCommandType(CommandType.OTHER).build()))
                  .build())
        .getUuid();
  }

  private void updateActivityStatus(String activityId, String appId, ExecutionStatus status) {
    activityHelperService.updateStatus(activityId, appId, status);
  }

  private WinRmConnectionAttributes setupWinrmCredentials(Ambiance ambiance, String connectionAttributes) {
    WinRmConnectionAttributes winRmConnectionAttributes = null;

    if (isEmpty(connectionAttributes)) {
      throw new ShellScriptException("WinRM Connection Attribute not provided in Shell Script Step",
          ErrorCode.SSH_CONNECTION_ERROR, Level.ERROR, WingsException.USER);
    }

    SettingAttribute keySettingAttribute = settingsService.get(connectionAttributes);
    if (keySettingAttribute == null) {
      keySettingAttribute = settingsService.getSettingAttributeByName(getAccountId(ambiance), connectionAttributes);
    }

    winRmConnectionAttributes = (WinRmConnectionAttributes) keySettingAttribute.getValue();

    if (winRmConnectionAttributes == null) {
      throw new ShellScriptException("WinRM Connection Attribute provided in Shell Script Step not found",
          ErrorCode.SSH_CONNECTION_ERROR, Level.ERROR, WingsException.USER);
    }
    return winRmConnectionAttributes;
  }
}
