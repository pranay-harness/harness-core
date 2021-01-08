package io.harness.steps.common.script;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;

import static java.util.Collections.emptyList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.shell.ShellScriptTaskResponseNG;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.shell.AccessType;
import io.harness.shell.AuthenticationScheme;
import io.harness.shell.KerberosConfig;
import io.harness.shell.ScriptType;
import io.harness.steps.StepUtils;
import io.harness.tasks.ResponseData;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class ShellScriptStep implements TaskExecutable<ShellScriptStepParameters> {
  public static final StepType STEP_TYPE = StepType.newBuilder().setType(ExecutionNodeType.SHELL_SCRIPT.name()).build();

  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Class<ShellScriptStepParameters> getStepParametersClass() {
    return ShellScriptStepParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, ShellScriptStepParameters stepParameters, StepInputPackage inputPackage) {
    String username = null;
    String keyPath = null;
    boolean keyless = false;
    Integer port = null;
    AccessType accessType = null;
    AuthenticationScheme authenticationScheme = null;
    String keyName = null;
    List<EncryptedDataDetail> winrmEdd = emptyList();
    List<EncryptedDataDetail> keyEncryptionDetails = emptyList();
    KerberosConfig kerberosConfig = null;

    // TODO: delete later
    //    software.wings.sm.states.ShellScriptState.ConnectionType connectionType = stepParameters.getConnectionType();
    //    if (connectionType == null) {
    //      connectionType = software.wings.sm.states.ShellScriptState.ConnectionType.SSH;
    //    }

    ScriptType scriptType = stepParameters.getShell().getScriptType();

    // TODO: execute on delegate
    //    if (!stepParameters.isExecuteOnDelegate()) {
    //      if (connectionType == software.wings.sm.states.ShellScriptState.ConnectionType.SSH) {
    //        String sshKeyRef = stepParameters.getSshKeyRef();
    //        if (isEmpty(sshKeyRef)) {
    //          throw new ShellScriptException("Valid SSH Connection Attribute not provided in Shell Script Step",
    //                  ErrorCode.SSH_CONNECTION_ERROR, Level.ERROR, WingsException.USER);
    //        }
    //
    //        SettingAttribute keySettingAttribute = settingsService.get(sshKeyRef);
    //        if (keySettingAttribute == null) {
    //          keySettingAttribute = settingsService.getSettingAttributeByName(getAccountId(ambiance), sshKeyRef);
    //        }
    //
    //        if (keySettingAttribute == null) {
    //          throw new ShellScriptException("SSH Connection Attribute provided in Shell Script Step not found",
    //                  ErrorCode.SSH_CONNECTION_ERROR, Level.ERROR, WingsException.USER);
    //        }
    //        hostConnectionAttributes = (HostConnectionAttributes) keySettingAttribute.getValue();
    //        username = ((HostConnectionAttributes) keySettingAttribute.getValue()).getUserName();
    //        keyPath = ((HostConnectionAttributes) keySettingAttribute.getValue()).getKeyPath();
    //        keyless = ((HostConnectionAttributes) keySettingAttribute.getValue()).isKeyless();
    //        port = ((HostConnectionAttributes) keySettingAttribute.getValue()).getSshPort();
    //        if (port == null) {
    //          port = 22;
    //        }
    //        accessType = ((HostConnectionAttributes) keySettingAttribute.getValue()).getAccessType();
    //        authenticationScheme = ((HostConnectionAttributes)
    //        keySettingAttribute.getValue()).getAuthenticationScheme(); kerberosConfig = hostConnectionAttributes !=
    //        null ? hostConnectionAttributes.getKerberosConfig() : null; keyName = keySettingAttribute.getUuid();
    //        keyEncryptionDetails = secretManager.getEncryptionDetails(
    //                (EncryptableSetting) keySettingAttribute.getValue(), getAppId(ambiance),
    //                ambiance.getPlanExecutionId());
    //
    //      } else if (connectionType == software.wings.sm.states.ShellScriptState.ConnectionType.WINRM) {
    //        winRmConnectionAttributes =
    //                setupWinrmCredentials(ambiance, ((ShellScriptStepParameters)
    //                stepParameters).getConnectionAttributes());
    //        username = winRmConnectionAttributes.getUsername();
    //        winrmEdd = secretManager.getEncryptionDetails(
    //                winRmConnectionAttributes, getAppId(ambiance), ambiance.getPlanExecutionId());
    //      }
    //    }

    String commandPath = getCommandPath(stepParameters, scriptType);

    // TODO: handle tags later, use task selectors
    //    List<String> tags = stepParameters.getTags();
    //    List<String> allTags = newArrayList();
    //    List<String> renderedTags = newArrayList();
    //    if (isNotEmpty(tags)) {
    //      allTags.addAll(tags);
    //    }
    //    if (isNotEmpty(allTags)) {
    //      renderedTags = trimStrings(renderedTags);
    //    }

    //    ShellScriptParametersBuilder shellScriptParameters =
    //            ShellScriptParameters.builder()
    //                    .accountId(getAccountId(ambiance))
    //                    .appId(getAppId(ambiance))
    //                    .host(stepParameters.getHost())
    //                    .connectionType(connectionType)
    //                    .winrmConnectionAttributes(winRmConnectionAttributes)
    //                    .winrmConnectionEncryptedDataDetails(winrmEdd)
    //                    .userName(username)
    //                    .keyEncryptedDataDetails(keyEncryptionDetails)
    //                    .workingDirectory(commandPath)
    //                    .scriptType(scriptType)
    //                    .script(stepParameters.getScriptString())
    //                    .executeOnDelegate(stepParameters.onDelegate.getValue())
    //                    .outputVars(stepParameters.getOutputVars())
    //                    .hostConnectionAttributes(hostConnectionAttributes)
    //                    .keyless(keyless)
    //                    .keyPath(keyPath)
    //                    .port(port)
    //                    .accessType(accessType)
    //                    .authenticationScheme(authenticationScheme)
    //                    .kerberosConfig(kerberosConfig)
    //                    .localOverrideFeatureFlag(
    //                            featureFlagService.isEnabled(LOCAL_DELEGATE_CONFIG_OVERRIDE, getAccountId(ambiance)))
    //                    .keyName(keyName)
    //                    .saveExecutionLogs(true);
    ShellScriptTaskResponseNG taskParameters = ShellScriptTaskResponseNG.builder().build();
    TaskData taskData = TaskData.builder()
                            .async(true)
                            .taskType(TaskType.SHELL_SCRIPT_TASK_NG.name())
                            .parameters(new Object[] {taskParameters})
                            .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                            .build();
    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer, new LinkedHashMap<>(),
        TaskCategory.DELEGATE_TASK_V1, Collections.emptyList());
  }

  private String getCommandPath(ShellScriptStepParameters stepParameters, ScriptType scriptType) {
    String commandPath = null;
    if (scriptType == ScriptType.BASH) {
      commandPath = "/tmp";
    } else if (scriptType == ScriptType.POWERSHELL) {
      commandPath = "%TEMP%";
      if (stepParameters.onDelegate.getValue()) {
        commandPath = "/tmp";
      }
    }
    return commandPath;
  }

  @Override
  public StepResponse handleTaskResult(
      Ambiance ambiance, ShellScriptStepParameters stepParameters, Map<String, ResponseData> response) {
    // TODO: handle response
    //    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    //    String activityId = response.keySet().iterator().next();
    //    ResponseData data = response.values().iterator().next();
    //    boolean saveSweepingOutputToContext = false;
    //    ExecutionStatus executionStatus = null;
    //    if (data instanceof CommandExecutionResult) {
    //      CommandExecutionResult commandExecutionResult = (CommandExecutionResult) data;
    //
    //      switch (commandExecutionResult.getStatus()) {
    //        case SUCCESS:
    //          executionStatus = ExecutionStatus.SUCCESS;
    //          stepResponseBuilder.status(Status.SUCCEEDED);
    //          break;
    //        case FAILURE:
    //          executionStatus = ExecutionStatus.FAILED;
    //          stepResponseBuilder.status(Status.FAILED);
    //          break;
    //        case RUNNING:
    //          executionStatus = ExecutionStatus.RUNNING;
    //          stepResponseBuilder.status(Status.RUNNING);
    //          break;
    //        case QUEUED:
    //          executionStatus = ExecutionStatus.QUEUED;
    //          stepResponseBuilder.status(Status.QUEUED);
    //          break;
    //        default:
    //          throw new ShellScriptException(
    //                  "Unhandled type CommandExecutionStatus: " + commandExecutionResult.getStatus().name(),
    //                  ErrorCode.SSH_CONNECTION_ERROR, Level.ERROR, WingsException.USER);
    //      }
    //
    //      FailureInfo.Builder failureInfoBuilder = FailureInfo.newBuilder();
    //      if (commandExecutionResult.getErrorMessage() != null) {
    //        failureInfoBuilder.setErrorMessage(commandExecutionResult.getErrorMessage());
    //      }
    //      stepResponseBuilder.failureInfo(failureInfoBuilder.build());
    //
    //      ScriptStateExecutionData scriptStateExecutionData =
    //              ScriptStateExecutionData.builder().activityId(activityId).build();
    //      if (commandExecutionResult.getStatus() == SUCCESS) {
    //        Map<String, String> sweepingOutputEnvVariables =
    //                ((ShellExecutionData) ((CommandExecutionResult) data).getCommandExecutionData())
    //                        .getSweepingOutputEnvVariables();
    //        scriptStateExecutionData.setSweepingOutputEnvVariables(sweepingOutputEnvVariables);
    //        scriptStateExecutionData.setStatus(ExecutionStatus.SUCCESS);
    //        saveSweepingOutputToContext = true;
    //      } else {
    //        scriptStateExecutionData.setStatus(ExecutionStatus.FAILED);
    //      }
    //      stepResponseBuilder.stepOutcome(StepResponse.StepOutcome.builder()
    //              .name("shellOutcome")
    //              .outcome(scriptStateExecutionData)
    //              .group(stepParameters.getSweepingOutputScope())
    //              .build());
    //    } else if (data instanceof ErrorNotifyResponseData) {
    //      stepResponseBuilder.status(Status.FAILED);
    //      stepResponseBuilder.failureInfo(
    //              FailureInfo.newBuilder().setErrorMessage(((ErrorNotifyResponseData)
    //              data).getErrorMessage()).build());
    //      return stepResponseBuilder.build();
    //    } else {
    //      log.error("Unhandled DelegateResponseData class " + data.getClass().getCanonicalName(), new Exception(""));
    //    }
    //
    //    StepResponse stepResponse = stepResponseBuilder.build();
    //
    //    if (saveSweepingOutputToContext && stepParameters.getSweepingOutputName() != null) {
    //      executionSweepingOutputService.consume(ambiance, stepParameters.getSweepingOutputName(),
    //              ShellScriptVariablesSweepingOutput.builder()
    //                      .variables(((ShellExecutionData) ((CommandExecutionResult) data).getCommandExecutionData())
    //                              .getSweepingOutputEnvVariables())
    //                      .build(),
    //              stepParameters.getSweepingOutputScope() == null ? ResolverUtils.GLOBAL_GROUP_SCOPE
    //                      : stepParameters.getSweepingOutputScope());
    //    }
    //
    //    return stepResponse;
    return StepResponse.builder().build();
  }

  private String getAccountId(Ambiance ambiance) {
    return ambiance.getSetupAbstractionsMap().get("accountId");
  }

  private String getAppId(Ambiance ambiance) {
    return ambiance.getSetupAbstractionsMap().get("appId");
  }

  //  private WinRmConnectionAttributes setupWinrmCredentials(Ambiance ambiance, String connectionAttributes) {
  //    WinRmConnectionAttributes winRmConnectionAttributes = null;
  //
  //    if (isEmpty(connectionAttributes)) {
  //      throw new ShellScriptException("WinRM Connection Attribute not provided in Shell Script Step",
  //              ErrorCode.SSH_CONNECTION_ERROR, Level.ERROR, WingsException.USER);
  //    }
  //
  //    SettingAttribute keySettingAttribute = settingsService.get(connectionAttributes);
  //    if (keySettingAttribute == null) {
  //      keySettingAttribute = settingsService.getSettingAttributeByName(getAccountId(ambiance), connectionAttributes);
  //    }
  //
  //    winRmConnectionAttributes = (WinRmConnectionAttributes) keySettingAttribute.getValue();
  //
  //    if (winRmConnectionAttributes == null) {
  //      throw new ShellScriptException("WinRM Connection Attribute provided in Shell Script Step not found",
  //              ErrorCode.SSH_CONNECTION_ERROR, Level.ERROR, WingsException.USER);
  //    }
  //    return winRmConnectionAttributes;
  //  }
}
