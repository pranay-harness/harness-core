package io.harness.delegate.task.citasks.vm;

import static io.harness.data.encoding.EncodingUtils.decodeBase64;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.citasks.vm.helper.CIVMConstants.RUNTEST_STEP_KIND;
import static io.harness.delegate.task.citasks.vm.helper.CIVMConstants.RUN_STEP_KIND;
import static io.harness.delegate.task.citasks.vm.helper.CIVMConstants.WORKDIR_VOLUME_NAME;

import static org.apache.commons.lang3.CharUtils.isAsciiAlphanumeric;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ci.CIExecuteStepTaskParams;
import io.harness.delegate.beans.ci.pod.SecretParams;
import io.harness.delegate.beans.ci.vm.CIVmExecuteStepTaskParams;
import io.harness.delegate.beans.ci.vm.VmTaskExecutionResponse;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepRequest;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepRequest.Config.ConfigBuilder;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepRequest.JunitReport;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepRequest.TestReport;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepResponse;
import io.harness.delegate.beans.ci.vm.steps.VmJunitTestReport;
import io.harness.delegate.beans.ci.vm.steps.VmPluginStep;
import io.harness.delegate.beans.ci.vm.steps.VmRunStep;
import io.harness.delegate.beans.ci.vm.steps.VmRunTestStep;
import io.harness.delegate.beans.ci.vm.steps.VmStepInfo;
import io.harness.delegate.beans.ci.vm.steps.VmUnitTestReport;
import io.harness.delegate.task.citasks.CIExecuteStepTaskHandler;
import io.harness.delegate.task.citasks.cik8handler.SecretSpecBuilder;
import io.harness.delegate.task.citasks.vm.helper.HttpHelper;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIVMExecuteStepTaskHandler implements CIExecuteStepTaskHandler {
  @Inject private HttpHelper httpHelper;
  @Inject private SecretSpecBuilder secretSpecBuilder;
  @NotNull private Type type = Type.VM;

  private static final String DOCKER_REGISTRY_V2 = "https://index.docker.io/v2/";
  private static final String DOCKER_REGISTRY_V1 = "https://index.docker.io/v1/";
  private static final String DOCKER_REGISTRY_ENV = "PLUGIN_REGISTRY";

  @Override
  public Type getType() {
    return type;
  }

  @Override
  public VmTaskExecutionResponse executeTaskInternal(CIExecuteStepTaskParams ciExecuteStepTaskParams) {
    CIVmExecuteStepTaskParams CIVmExecuteStepTaskParams = (CIVmExecuteStepTaskParams) ciExecuteStepTaskParams;
    log.info(
        "Received request to execute step with stage runtime ID {}", CIVmExecuteStepTaskParams.getStageRuntimeId());
    return callRunnerForStepExecution(CIVmExecuteStepTaskParams);
  }

  private VmTaskExecutionResponse callRunnerForStepExecution(CIVmExecuteStepTaskParams params) {
    try {
      Response<ExecuteStepResponse> response = httpHelper.executeStepWithRetries(convert(params));
      if (!response.isSuccessful()) {
        return VmTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.FAILURE).build();
      }

      if (isEmpty(response.body().getError())) {
        return VmTaskExecutionResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
      } else {
        return VmTaskExecutionResponse.builder()
            .commandExecutionStatus(CommandExecutionStatus.FAILURE)
            .errorMessage(response.body().getError())
            .build();
      }
    } catch (Exception e) {
      log.error("Failed to execute step in runner", e);
      return VmTaskExecutionResponse.builder()
          .commandExecutionStatus(CommandExecutionStatus.FAILURE)
          .errorMessage(e.getMessage())
          .build();
    }
  }

  private ExecuteStepRequest convert(CIVmExecuteStepTaskParams params) {
    ExecuteStepRequest.VolumeMount workdirVol =
        ExecuteStepRequest.VolumeMount.builder().name(WORKDIR_VOLUME_NAME).path(params.getWorkingDir()).build();

    ConfigBuilder configBuilder = ExecuteStepRequest.Config.builder()
                                      .id(getIdentifier(params.getStepRuntimeId()))
                                      .name(params.getStepId())
                                      .logKey(params.getLogKey())
                                      .workingDir(params.getWorkingDir())
                                      .volumeMounts(Collections.singletonList(workdirVol));
    if (params.getStepInfo().getType() == VmStepInfo.Type.RUN) {
      VmRunStep runStep = (VmRunStep) params.getStepInfo();
      setRunConfig(runStep, configBuilder);
    } else if (params.getStepInfo().getType() == VmStepInfo.Type.PLUGIN) {
      VmPluginStep pluginStep = (VmPluginStep) params.getStepInfo();
      setPluginConfig(pluginStep, configBuilder);
    } else if (params.getStepInfo().getType() == VmStepInfo.Type.RUN_TEST) {
      VmRunTestStep runTestStep = (VmRunTestStep) params.getStepInfo();
      setRunTestConfig(runTestStep, configBuilder);
    }
    if (isNotEmpty(params.getSecrets())) {
      params.getSecrets().forEach(secret -> configBuilder.secret(secret));
    }
    return ExecuteStepRequest.builder()
        .poolId(params.getPoolId())
        .ipAddress(params.getIpAddress())
        .config(configBuilder.build())
        .build();
  }

  private void setRunConfig(VmRunStep runStep, ConfigBuilder configBuilder) {
    configBuilder.kind(RUN_STEP_KIND)
        .runConfig(ExecuteStepRequest.RunConfig.builder()
                       .command(Collections.singletonList(runStep.getCommand()))
                       .entrypoint(runStep.getEntrypoint())
                       .build())
        .image(runStep.getImage())
        .pull(runStep.getPullPolicy())
        .user(runStep.getRunAsUser())
        .envs(runStep.getEnvVariables())
        .privileged(runStep.isPrivileged())
        .outputVars(runStep.getOutputVariables())
        .testReport(convertTestReport(runStep.getUnitTestReport()))
        .timeout(runStep.getTimeoutSecs());
  }

  private void setPluginConfig(VmPluginStep pluginStep, ConfigBuilder configBuilder) {
    Map<String, String> env = new HashMap<>();
    List<String> secrets = new ArrayList<>();
    if (isNotEmpty(pluginStep.getEnvVariables())) {
      env = pluginStep.getEnvVariables();
    }

    if (pluginStep.getConnector() != null) {
      Map<String, SecretParams> secretVars = secretSpecBuilder.decryptConnectorSecret(pluginStep.getConnector());
      for (Map.Entry<String, SecretParams> entry : secretVars.entrySet()) {
        String secret = new String(decodeBase64(entry.getValue().getValue()));
        String key = entry.getKey();

        // Drone docker plugin does not work with v2 registry
        if (key.equals(DOCKER_REGISTRY_ENV) && secret.equals(DOCKER_REGISTRY_V2)) {
          secret = DOCKER_REGISTRY_V1;
        }
        env.put(entry.getKey(), secret);
        secrets.add(secret);
      }
    }
    configBuilder.kind(RUN_STEP_KIND)
        .runConfig(ExecuteStepRequest.RunConfig.builder().build())
        .image(pluginStep.getImage())
        .pull(pluginStep.getPullPolicy())
        .user(pluginStep.getRunAsUser())
        .secrets(secrets)
        .envs(pluginStep.getEnvVariables())
        .privileged(pluginStep.isPrivileged())
        .testReport(convertTestReport(pluginStep.getUnitTestReport()))
        .timeout(pluginStep.getTimeoutSecs());
  }

  private void setRunTestConfig(VmRunTestStep runTestStep, ConfigBuilder configBuilder) {
    configBuilder.kind(RUNTEST_STEP_KIND)
        .runTestConfig(ExecuteStepRequest.RunTestConfig.builder()
                           .args(runTestStep.getArgs())
                           .entrypoint(runTestStep.getEntrypoint())
                           .preCommand(runTestStep.getPreCommand())
                           .postCommand(runTestStep.getPostCommand())
                           .buildTool(runTestStep.getBuildTool())
                           .language(runTestStep.getLanguage())
                           .packages(runTestStep.getLanguage())
                           .runOnlySelectedTests(runTestStep.isRunOnlySelectedTests())
                           .testAnnotations(runTestStep.getTestAnnotations())
                           .build())
        .image(runTestStep.getImage())
        .pull(runTestStep.getPullPolicy())
        .user(runTestStep.getRunAsUser())
        .envs(runTestStep.getEnvVariables())
        .privileged(runTestStep.isPrivileged())
        .outputVars(runTestStep.getOutputVariables())
        .testReport(convertTestReport(runTestStep.getUnitTestReport()))
        .timeout(runTestStep.getTimeoutSecs());
  }

  private TestReport convertTestReport(VmUnitTestReport unitTestReport) {
    if (unitTestReport == null) {
      return null;
    }

    if (unitTestReport.getType() != VmUnitTestReport.Type.JUNIT) {
      return null;
    }

    VmJunitTestReport junitTestReport = (VmJunitTestReport) unitTestReport;
    return TestReport.builder()
        .kind("Junit")
        .junitReport(JunitReport.builder().paths(junitTestReport.getPaths()).build())
        .build();
  }

  public String getIdentifier(String identifier) {
    StringBuilder sb = new StringBuilder(15);
    for (char c : identifier.toCharArray()) {
      if (isAsciiAlphanumeric(c)) {
        sb.append(c);
      }
      if (sb.length() == 15) {
        return sb.toString();
      }
    }
    return sb.toString();
  }
}
