package io.harness.steps.shellscript;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.util.Collections.emptyList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.shell.ShellScriptTaskParametersNG;
import io.harness.delegate.task.shell.ShellScriptTaskParametersNG.ShellScriptTaskParametersNGBuilder;
import io.harness.eraro.Level;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.k8s.K8sConstants;
import io.harness.ng.core.NGAccess;
import io.harness.ng.core.api.SecretCrudService;
import io.harness.ng.core.dto.secrets.SSHKeySpecDTO;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.ng.core.dto.secrets.SecretResponseWrapper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;
import io.harness.pms.yaml.ParameterField;
import io.harness.secretmanagerclient.services.SshKeySpecDTOHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.shell.ScriptType;
import io.harness.steps.OutputExpressionConstants;
import io.harness.utils.IdentifierRefHelper;

import software.wings.exception.ShellScriptException;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDC)
@Slf4j
public class ShellScriptHelperServiceImpl implements ShellScriptHelperService {
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;
  @Inject private SecretCrudService secretCrudService;
  @Inject private SshKeySpecDTOHelper sshKeySpecDTOHelper;
  @Inject private ShellScriptHelperService shellScriptHelperService;

  @Override
  public Map<String, String> getEnvironmentVariables(Map<String, Object> inputVariables) {
    if (EmptyPredicate.isEmpty(inputVariables)) {
      return new HashMap<>();
    }
    Map<String, String> res = new LinkedHashMap<>();
    inputVariables.forEach((key, value) -> {
      if (value instanceof ParameterField) {
        ParameterField<?> parameterFieldValue = (ParameterField<?>) value;
        if (parameterFieldValue.getValue() == null) {
          throw new InvalidRequestException(String.format("Env. variable [%s] value found to be null", key));
        }
        res.put(key, parameterFieldValue.getValue().toString());
      } else if (value instanceof String) {
        res.put(key, (String) value);
      } else {
        log.error(String.format(
            "Value other than String or ParameterField found for env. variable [%s]. value: [%s]", key, value));
      }
    });
    return res;
  }

  @Override
  public List<String> getOutputVars(Map<String, Object> outputVariables) {
    if (EmptyPredicate.isEmpty(outputVariables)) {
      return emptyList();
    }

    List<String> outputVars = new ArrayList<>();
    outputVariables.forEach((key, val) -> {
      if (val instanceof ParameterField) {
        ParameterField<?> parameterFieldValue = (ParameterField<?>) val;
        if (parameterFieldValue.getValue() == null) {
          throw new InvalidRequestException(String.format("Output variable [%s] value found to be null", key));
        }
        outputVars.add(((ParameterField<?>) val).getValue().toString());
      } else if (val instanceof String) {
        outputVars.add((String) val);
      } else {
        log.error(String.format(
            "Value other than String or ParameterField found for output variable [%s]. value: [%s]", key, val));
      }
    });
    return outputVars;
  }

  @Override
  public K8sInfraDelegateConfig getK8sInfraDelegateConfig(@Nonnull Ambiance ambiance, @Nonnull String shellScript) {
    if (shellScript.contains(K8sConstants.HARNESS_KUBE_CONFIG_PATH)) {
      OptionalSweepingOutput optionalSweepingOutput = executionSweepingOutputService.resolveOptional(ambiance,
          RefObjectUtils.getSweepingOutputRefObject(OutputExpressionConstants.K8S_INFRA_DELEGATE_CONFIG_OUTPUT_NAME));
      if (optionalSweepingOutput.isFound()) {
        K8sInfraDelegateConfigOutput k8sInfraDelegateConfigOutput =
            (K8sInfraDelegateConfigOutput) optionalSweepingOutput.getOutput();
        return k8sInfraDelegateConfigOutput.getK8sInfraDelegateConfig();
      }
    }
    return null;
  }

  @Override
  public void prepareTaskParametersForExecutionTarget(@Nonnull Ambiance ambiance,
      @Nonnull ShellScriptStepParameters shellScriptStepParameters,
      @Nonnull ShellScriptTaskParametersNGBuilder taskParametersNGBuilder) {
    if (!shellScriptStepParameters.onDelegate.getValue()) {
      ExecutionTarget executionTarget = shellScriptStepParameters.getExecutionTarget();
      validateExecutionTarget(executionTarget);
      String sshKeyRef = executionTarget.getConnectorRef().getValue();

      IdentifierRef identifierRef =
          IdentifierRefHelper.getIdentifierRef(sshKeyRef, AmbianceUtils.getAccountId(ambiance),
              AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));
      Optional<SecretResponseWrapper> secretResponseWrapper =
          secretCrudService.get(identifierRef.getAccountIdentifier(), identifierRef.getOrgIdentifier(),
              identifierRef.getProjectIdentifier(), identifierRef.getIdentifier());
      if (!secretResponseWrapper.isPresent()) {
        throw new InvalidRequestException("No secret configured with identifier: " + sshKeyRef);
      }
      SecretDTOV2 secret = secretResponseWrapper.get().getSecret();

      SSHKeySpecDTO secretSpec = (SSHKeySpecDTO) secret.getSpec();
      NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
      List<EncryptedDataDetail> sshKeyEncryptionDetails =
          sshKeySpecDTOHelper.getSSHKeyEncryptionDetails(secretSpec, ngAccess);

      taskParametersNGBuilder.sshKeySpecDTO(secretSpec)
          .encryptionDetails(sshKeyEncryptionDetails)
          .host(executionTarget.getHost().getValue());
    }
  }

  private void validateExecutionTarget(ExecutionTarget executionTarget) {
    if (executionTarget == null) {
      throw new InvalidRequestException("Execution Target can't be empty with on delegate set to false");
    }
    if (ParameterField.isNull(executionTarget.getConnectorRef())
        || StringUtils.isEmpty(executionTarget.getConnectorRef().getValue())) {
      throw new InvalidRequestException("Connector Ref in Execution Target can't be empty");
    }
    if (ParameterField.isNull(executionTarget.getHost()) || StringUtils.isEmpty(executionTarget.getHost().getValue())) {
      throw new InvalidRequestException("Host in Execution Target can't be empty");
    }
  }

  @Override
  public String getShellScript(@Nonnull ShellScriptStepParameters stepParameters) {
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

  @Override
  public String getWorkingDirectory(@Nonnull ShellScriptStepParameters stepParameters, @Nonnull ScriptType scriptType) {
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
  public ShellScriptTaskParametersNG buildShellScriptTaskParametersNG(
      @Nonnull Ambiance ambiance, @Nonnull ShellScriptStepParameters shellScriptStepParameters) {
    ScriptType scriptType = shellScriptStepParameters.getShell().getScriptType();
    ShellScriptTaskParametersNGBuilder taskParametersNGBuilder = ShellScriptTaskParametersNG.builder();

    String shellScript = shellScriptHelperService.getShellScript(shellScriptStepParameters);
    taskParametersNGBuilder.k8sInfraDelegateConfig(
        shellScriptHelperService.getK8sInfraDelegateConfig(ambiance, shellScript));
    shellScriptHelperService.prepareTaskParametersForExecutionTarget(
        ambiance, shellScriptStepParameters, taskParametersNGBuilder);

    return taskParametersNGBuilder.accountId(AmbianceUtils.getAccountId(ambiance))
        .executeOnDelegate(shellScriptStepParameters.onDelegate.getValue())
        .environmentVariables(
            shellScriptHelperService.getEnvironmentVariables(shellScriptStepParameters.getEnvironmentVariables()))
        .executionId(AmbianceUtils.obtainCurrentRuntimeId(ambiance))
        .outputVars(shellScriptHelperService.getOutputVars(shellScriptStepParameters.getOutputVariables()))
        .script(shellScript)
        .scriptType(scriptType)
        .workingDirectory(shellScriptHelperService.getWorkingDirectory(shellScriptStepParameters, scriptType))
        .build();
  }

  @Override
  public ShellScriptOutcome prepareShellScriptOutcome(
      Map<String, String> sweepingOutputEnvVariables, Map<String, Object> outputVariables) {
    if (outputVariables == null || sweepingOutputEnvVariables == null) {
      return null;
    }
    Map<String, String> resolvedOutputVariables = new HashMap<>();
    outputVariables.keySet().forEach(name -> {
      Object value = ((ParameterField<?>) outputVariables.get(name)).getValue();
      resolvedOutputVariables.put(name, sweepingOutputEnvVariables.get(value));
    });
    return ShellScriptOutcome.builder().outputVariables(resolvedOutputVariables).build();
  }
}
