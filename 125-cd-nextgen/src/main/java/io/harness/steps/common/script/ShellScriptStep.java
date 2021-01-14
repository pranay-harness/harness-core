package io.harness.steps.common.script;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.shell.ShellScriptTaskNG;
import io.harness.delegate.task.shell.ShellScriptTaskParametersNG;
import io.harness.delegate.task.shell.ShellScriptTaskResponseNG;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.WingsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.steps.executables.TaskExecutable;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.serializer.KryoSerializer;
import io.harness.shell.ScriptType;
import io.harness.shell.ShellExecutionData;
import io.harness.steps.StepUtils;
import io.harness.tasks.ResponseData;
import io.harness.yaml.core.variables.NGVariable;

import software.wings.beans.TaskType;
import software.wings.exception.ShellScriptException;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class ShellScriptStep implements TaskExecutable<ShellScriptStepParameters> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.SHELL_SCRIPT.getYamlType()).build();

  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Class<ShellScriptStepParameters> getStepParametersClass() {
    return ShellScriptStepParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, ShellScriptStepParameters stepParameters, StepInputPackage inputPackage) {
    //    TODO: initialize later as needed
    //    String username = null;
    //    String keyPath = null;
    //    boolean keyless = false;
    //    Integer port = null;
    //    AccessType accessType = null;
    //    AuthenticationScheme authenticationScheme = null;
    //    String keyName = null;
    //    List<EncryptedDataDetail> winrmEdd = emptyList();
    //    List<EncryptedDataDetail> keyEncryptionDetails = emptyList();
    //    KerberosConfig kerberosConfig = null;

    ScriptType scriptType = stepParameters.getShell().getScriptType();

    //        TODO: execute on delegate
    //        if (!stepParameters.isExecuteOnDelegate()) {
    //          if (connectionType == software.wings.sm.states.ShellScriptState.ConnectionType.SSH) {
    //            String sshKeyRef = stepParameters.getSshKeyRef();
    //            if (isEmpty(sshKeyRef)) {
    //              throw new ShellScriptException("Valid SSH Connection Attribute not provided in Shell Script Step",
    //                      ErrorCode.SSH_CONNECTION_ERROR, Level.ERROR, WingsException.USER);
    //            }
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

    String workingDirectory = getWorkingDirectory(stepParameters, scriptType);
    Map<String, String> environmentVariables = getEnvironmentVariables(stepParameters.getEnvironmentVariables());
    List<String> outputVars = getOutputVars(stepParameters.getOutputVariables());

    // TODO: handle tags later, use task selectors
    // List<String> tags = stepParameters.getTags();
    // List<String> allTags = newArrayList();
    // List<String> renderedTags = newArrayList();
    // if (isNotEmpty(tags)) {
    //   allTags.addAll(tags);
    // }
    // if (isNotEmpty(allTags)) {
    //   renderedTags = trimStrings(renderedTags);
    // }

    ShellScriptTaskParametersNG taskParameters =
        ShellScriptTaskParametersNG.builder()
            .accountId(AmbianceHelper.getAccountId(ambiance))
            .executeOnDelegate(stepParameters.onDelegate.getValue())
            .environmentVariables(environmentVariables)
            .executionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
            // TODO: Pass infra delegate config as well for kubeConfigContent
            // .k8sInfraDelegateConfig()
            .outputVars(outputVars)
            .script(((ShellScriptInlineSource) stepParameters.getSource().getSpec()).getScript().getValue())
            .scriptType(scriptType)
            .workingDirectory(workingDirectory)
            .build();

    TaskData taskData = TaskData.builder()
                            .async(true)
                            .taskType(TaskType.SHELL_SCRIPT_TASK_NG.name())
                            .parameters(new Object[] {taskParameters})
                            .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                            .build();
    return StepUtils.prepareTaskRequest(ambiance, taskData, kryoSerializer, new LinkedHashMap<>(),
        TaskCategory.DELEGATE_TASK_V2, singletonList(ShellScriptTaskNG.COMMAND_UNIT));
  }

  private Map<String, String> getEnvironmentVariables(List<NGVariable> inputVariables) {
    if (EmptyPredicate.isEmpty(inputVariables)) {
      return emptyMap();
    }

    // TODO: handle for secret type later
    return inputVariables.stream().collect(Collectors.toMap(
        NGVariable::getName, inputVariable -> String.valueOf(inputVariable.getValue().getValue()), (a, b) -> b));
  }

  private List<String> getOutputVars(List<NGVariable> outputVariables) {
    if (EmptyPredicate.isEmpty(outputVariables)) {
      return emptyList();
    }

    List<String> outputVars = new ArrayList<>();
    for (NGVariable inputVariable : outputVariables) {
      outputVars.add((String) inputVariable.getValue().getValue());
    }
    return outputVars;
  }

  private String getWorkingDirectory(ShellScriptStepParameters stepParameters, ScriptType scriptType) {
    if (stepParameters.getExecutionTarget() != null
        && stepParameters.getExecutionTarget().getWorkingDirectory() != null) {
      return stepParameters.getExecutionTarget().getWorkingDirectory().getValue();
    }
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
    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    ResponseData responseData = response.values().iterator().next();
    if (responseData instanceof ShellScriptTaskResponseNG) {
      ShellScriptTaskResponseNG taskResponse = (ShellScriptTaskResponseNG) responseData;

      switch (taskResponse.getExecuteCommandResponse().getStatus()) {
        case SUCCESS:
          stepResponseBuilder.status(Status.SUCCEEDED);
          break;
        case FAILURE:
          stepResponseBuilder.status(Status.FAILED);
          break;
        case RUNNING:
          stepResponseBuilder.status(Status.RUNNING);
          break;
        case QUEUED:
          stepResponseBuilder.status(Status.QUEUED);
          break;
        default:
          throw new ShellScriptException(
              "Unhandled type CommandExecutionStatus: " + taskResponse.getExecuteCommandResponse().getStatus().name(),
              ErrorCode.SSH_CONNECTION_ERROR, Level.ERROR, WingsException.USER);
      }

      FailureInfo.Builder failureInfoBuilder = FailureInfo.newBuilder();
      if (taskResponse.getErrorMessage() != null) {
        failureInfoBuilder.setErrorMessage(taskResponse.getErrorMessage());
      }
      stepResponseBuilder.failureInfo(failureInfoBuilder.build());

      if (taskResponse.getExecuteCommandResponse().getStatus() == CommandExecutionStatus.SUCCESS) {
        Map<String, String> sweepingOutputEnvVariables =
            ((ShellExecutionData) taskResponse.getExecuteCommandResponse().getCommandExecutionData())
                .getSweepingOutputEnvVariables();

        if (stepParameters.getOutputVariables() != null) {
          ShellScriptOutcome shellScriptOutcome = prepareShellScriptOutcome(stepParameters, sweepingOutputEnvVariables);
          stepResponseBuilder.stepOutcome(StepResponse.StepOutcome.builder()
                                              .name(OutcomeExpressionConstants.OUTPUT)
                                              .outcome(shellScriptOutcome)
                                              .build());
        }
      }
    } else if (responseData instanceof ErrorNotifyResponseData) {
      stepResponseBuilder.status(Status.FAILED);
      stepResponseBuilder.failureInfo(
          FailureInfo.newBuilder().setErrorMessage(((ErrorNotifyResponseData) responseData).getErrorMessage()).build());
      return stepResponseBuilder.build();
    } else {
      log.error(
          "Unhandled DelegateResponseData class " + responseData.getClass().getCanonicalName(), new Exception(""));
    }
    return stepResponseBuilder.build();
  }

  private ShellScriptOutcome prepareShellScriptOutcome(
      ShellScriptStepParameters stepParameters, Map<String, String> sweepingOutputEnvVariables) {
    Map<String, String> outputVariables = new HashMap<>();
    for (NGVariable outputVariable : stepParameters.getOutputVariables()) {
      outputVariables.put(
          outputVariable.getName(), sweepingOutputEnvVariables.get(outputVariable.getValue().getValue()));
    }
    return ShellScriptOutcome.builder().outputVariables(outputVariables).build();
  }
}
