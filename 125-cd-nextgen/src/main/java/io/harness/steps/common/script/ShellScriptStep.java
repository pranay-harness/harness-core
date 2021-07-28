package io.harness.steps.common.script;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.common.step.StepHelper;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.common.NGTimeConversionHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.task.shell.ShellScriptTaskNG;
import io.harness.delegate.task.shell.ShellScriptTaskParametersNG;
import io.harness.delegate.task.shell.ShellScriptTaskParametersNG.ShellScriptTaskParametersNGBuilder;
import io.harness.delegate.task.shell.ShellScriptTaskResponseNG;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.k8s.K8sConstants;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.UnitProgress;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.pms.sdk.core.steps.io.StepResponse.StepResponseBuilder;
import io.harness.pms.yaml.ParameterField;
import io.harness.secretmanagerclient.services.SshKeySpecDTOHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.shell.ScriptType;
import io.harness.shell.ShellExecutionData;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;
import io.harness.utils.IdentifierRefHelper;

import software.wings.beans.TaskType;
import software.wings.exception.ShellScriptException;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
public class ShellScriptStep extends TaskExecutableWithRollback<ShellScriptTaskResponseNG> {
  public static final StepType STEP_TYPE = StepType.newBuilder()
                                               .setType(ExecutionNodeType.SHELL_SCRIPT.getYamlType())
                                               .setStepCategory(StepCategory.STEP)
                                               .build();

  @Inject private KryoSerializer kryoSerializer;
  @Inject private SecretCrudService secretCrudService;
  @Inject private SshKeySpecDTOHelper sshKeySpecDTOHelper;
  @Inject private OutcomeService outcomeService;
  @Inject private K8sStepHelper k8sStepHelper;
  @Inject private StepHelper stepHelper;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    ShellScriptStepParameters shellScriptStepParameters = (ShellScriptStepParameters) stepParameters.getSpec();

    ScriptType scriptType = shellScriptStepParameters.getShell().getScriptType();
    ShellScriptTaskParametersNGBuilder taskParametersNGBuilder = ShellScriptTaskParametersNG.builder();

    String shellScript = getShellScript(shellScriptStepParameters);

    if (shellScript.contains(K8sConstants.HARNESS_KUBE_CONFIG_PATH)) {
      InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
          ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));
      if (infrastructureOutcome == null) {
        throw new InvalidRequestException("Infrastructure not available");
      }
      taskParametersNGBuilder.k8sInfraDelegateConfig(
          k8sStepHelper.getK8sInfraDelegateConfig(infrastructureOutcome, ambiance));
    }

    if (!shellScriptStepParameters.onDelegate.getValue()) {
      ExecutionTarget executionTarget = shellScriptStepParameters.getExecutionTarget();
      if (executionTarget == null) {
        throw new InvalidRequestException("Execution Target can't be empty with on delegate set to false");
      }
      String sshKeyRef = executionTarget.getConnectorRef().getValue();

      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(sshKeyRef, AmbianceHelper.getAccountId(ambiance),
              AmbianceHelper.getOrgIdentifier(ambiance), AmbianceHelper.getProjectIdentifier(ambiance));
      Optional<SecretResponseWrapper> secretResponseWrapper =
          secretCrudService.get(identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
              identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
      if (!secretResponseWrapper.isPresent()) {
        throw new InvalidRequestException("No secret configured with identifier: " + sshKeyRef);
      }
      SecretDTOV2 secret = secretResponseWrapper.get().getSecret();

      SSHKeySpecDTO secretSpec = (SSHKeySpecDTO) secret.getSpec();
      NGAccess ngAccess = AmbianceHelper.getNgAccess(ambiance);
      List<EncryptedDataDetail> sshKeyEncryptionDetails =
          sshKeySpecDTOHelper.getSSHKeyEncryptionDetails(secretSpec, ngAccess);

      taskParametersNGBuilder.sshKeySpecDTO(secretSpec)
          .encryptionDetails(sshKeyEncryptionDetails)
          .host(executionTarget.getHost().getValue());
    }

    String workingDirectory = getWorkingDirectory(shellScriptStepParameters, scriptType);
    Map<String, String> environmentVariables =
        getEnvironmentVariables(shellScriptStepParameters.getEnvironmentVariables());
    List<String> outputVars = getOutputVars(shellScriptStepParameters.getOutputVariables());

    ShellScriptTaskParametersNG taskParameters =
        taskParametersNGBuilder.accountId(AmbianceHelper.getAccountId(ambiance))
            .executeOnDelegate(shellScriptStepParameters.onDelegate.getValue())
            .environmentVariables(environmentVariables)
            .executionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
            .outputVars(outputVars)
            .script(shellScript)
            .scriptType(scriptType)
            .workingDirectory(workingDirectory)
            .build();

    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.SHELL_SCRIPT_TASK_NG.name())
            .parameters(new Object[] {taskParameters})
            .timeout(NGTimeConversionHelper.convertTimeStringToMilliseconds(stepParameters.getTimeout().getValue()))
            .build();
    String taskName = TaskType.SHELL_SCRIPT_TASK_NG.getDisplayName();
    return StepUtils.prepareCDTaskRequest(ambiance, taskData, kryoSerializer,
        singletonList(ShellScriptTaskNG.COMMAND_UNIT), taskName,
        TaskSelectorYaml.toTaskSelector(shellScriptStepParameters.delegateSelectors.getValue()),
        stepHelper.getEnvironmentType(ambiance));
  }

  private String getShellScript(ShellScriptStepParameters stepParameters) {
    ShellScriptInlineSource shellScriptInlineSource = (ShellScriptInlineSource) stepParameters.getSource().getSpec();
    if (shellScriptInlineSource.getScript().isExpression()) {
      final long maxLimit = 10;
      List<String> variables =
          EngineExpressionEvaluator.findExpressions(shellScriptInlineSource.getScript().getExpressionValue())
              .stream()
              .limit(maxLimit)
              .collect(Collectors.toList());
      throw new ShellScriptException(
          "Script contains unresolved expressions " + variables, null, Level.ERROR, WingsException.USER);
    }

    return shellScriptInlineSource.getScript().getValue();
  }

  private Map<String, String> getEnvironmentVariables(Map<String, Object> inputVariables) {
    if (EmptyPredicate.isEmpty(inputVariables)) {
      return new HashMap<>();
    }
    Map<String, String> res = new LinkedHashMap<>();
    inputVariables.keySet().forEach(
        key -> res.put(key, ((ParameterField<?>) inputVariables.get(key)).getValue().toString()));
    return res;
  }

  private List<String> getOutputVars(Map<String, Object> outputVariables) {
    if (EmptyPredicate.isEmpty(outputVariables)) {
      return emptyList();
    }

    List<String> outputVars = new ArrayList<>();
    outputVariables.values().forEach(val -> {
      if (val instanceof ParameterField) {
        outputVars.add(((ParameterField<?>) val).getValue().toString());
      }
    });
    return outputVars;
  }

  private String getWorkingDirectory(ShellScriptStepParameters stepParameters, ScriptType scriptType) {
    if (stepParameters.getExecutionTarget() != null && stepParameters.getExecutionTarget().getWorkingDirectory() != null
        && EmptyPredicate.isNotEmpty(stepParameters.getExecutionTarget().getWorkingDirectory().getValue())) {
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
  public StepResponse handleTaskResult(Ambiance ambiance, StepElementParameters stepParameters,
      ThrowingSupplier<ShellScriptTaskResponseNG> responseSupplier) throws Exception {
    StepResponseBuilder stepResponseBuilder = StepResponse.builder();
    ShellScriptTaskResponseNG taskResponse = responseSupplier.get();
    ShellScriptStepParameters shellScriptStepParameters = (ShellScriptStepParameters) stepParameters.getSpec();
    List<UnitProgress> unitProgresses = taskResponse.getUnitProgressData() == null
        ? emptyList()
        : taskResponse.getUnitProgressData().getUnitProgresses();
    stepResponseBuilder.unitProgressList(unitProgresses);

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

      if (shellScriptStepParameters.getOutputVariables() != null) {
        ShellScriptOutcome shellScriptOutcome =
            prepareShellScriptOutcome(shellScriptStepParameters, sweepingOutputEnvVariables);
        stepResponseBuilder.stepOutcome(StepResponse.StepOutcome.builder()
                                            .name(OutcomeExpressionConstants.OUTPUT)
                                            .outcome(shellScriptOutcome)
                                            .build());
      }
    }
    return stepResponseBuilder.build();
  }

  private ShellScriptOutcome prepareShellScriptOutcome(
      ShellScriptStepParameters stepParameters, Map<String, String> sweepingOutputEnvVariables) {
    Map<String, String> outputVariables = new HashMap<>();
    stepParameters.getOutputVariables().keySet().forEach(name -> {
      Object value = ((ParameterField<?>) stepParameters.getOutputVariables().get(name)).getValue();
      outputVariables.put(name, sweepingOutputEnvVariables.get(value));
    });
    return ShellScriptOutcome.builder().outputVariables(outputVariables).build();
  }
}
