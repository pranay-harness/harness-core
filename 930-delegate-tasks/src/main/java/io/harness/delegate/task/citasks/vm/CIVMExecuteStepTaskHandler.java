package io.harness.delegate.task.citasks.vm;

import static io.harness.delegate.task.citasks.vm.helper.CIVMConstants.RUNTEST_STEP_KIND;
import static io.harness.delegate.task.citasks.vm.helper.CIVMConstants.RUN_STEP_KIND;
import static io.harness.delegate.task.citasks.vm.helper.CIVMConstants.WORKDIR_VOLUME_NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.ci.CIExecuteStepTaskParams;
import io.harness.delegate.beans.ci.vm.CIVmExecuteStepTaskParams;
import io.harness.delegate.beans.ci.vm.VmTaskExecutionResponse;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepRequest;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepRequest.Config.ConfigBuilder;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepRequest.JunitReport;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepRequest.TestReport;
import io.harness.delegate.beans.ci.vm.runner.ExecuteStepResponse;
import io.harness.delegate.beans.ci.vm.steps.JunitTestReport;
import io.harness.delegate.beans.ci.vm.steps.PluginStep;
import io.harness.delegate.beans.ci.vm.steps.RunStep;
import io.harness.delegate.beans.ci.vm.steps.RunTestStep;
import io.harness.delegate.beans.ci.vm.steps.StepInfo;
import io.harness.delegate.beans.ci.vm.steps.UnitTestReport;
import io.harness.delegate.task.citasks.CIExecuteStepTaskHandler;
import io.harness.delegate.task.citasks.vm.helper.HttpHelper;
import io.harness.logging.CommandExecutionStatus;

import com.google.inject.Inject;
import java.util.Collections;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Response;

@Slf4j
@OwnedBy(HarnessTeam.CI)
public class CIVMExecuteStepTaskHandler implements CIExecuteStepTaskHandler {
  @Inject private HttpHelper httpHelper;
  @NotNull private Type type = Type.VM;

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

      if (response.body().getError().equals("")) {
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
                                      .id(params.getStepRuntimeId())
                                      .name(params.getStepId())
                                      .logKey(params.getLogKey())
                                      .workingDir(params.getWorkingDir())
                                      .timeout(params.getTimeoutSecs())
                                      .volumeMounts(Collections.singletonList(workdirVol));
    if (params.getStepInfo().getType() == StepInfo.Type.RUN) {
      RunStep runStep = (RunStep) params.getStepInfo();
      setRunConfig(runStep, configBuilder);
    } else if (params.getStepInfo().getType() == StepInfo.Type.PLUGIN) {
      PluginStep pluginStep = (PluginStep) params.getStepInfo();
      setPluginConfig(pluginStep, configBuilder);
    } else if (params.getStepInfo().getType() == StepInfo.Type.RUN_TEST) {
      RunTestStep runTestStep = (RunTestStep) params.getStepInfo();
      setRunTestConfig(runTestStep, configBuilder);
    }
    return ExecuteStepRequest.builder().ipAddress(params.getIpAddress()).config(configBuilder.build()).build();
  }

  private void setRunConfig(RunStep runStep, ConfigBuilder configBuilder) {
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
        .testReport(convertTestReport(runStep.getUnitTestReport()));
  }

  private void setPluginConfig(PluginStep pluginStep, ConfigBuilder configBuilder) {
    configBuilder.kind(RUN_STEP_KIND)
        .runConfig(ExecuteStepRequest.RunConfig.builder().build())
        .image(pluginStep.getImage())
        .pull(pluginStep.getPullPolicy())
        .user(pluginStep.getRunAsUser())
        .envs(pluginStep.getEnvVariables())
        .privileged(pluginStep.isPrivileged())
        .testReport(convertTestReport(pluginStep.getUnitTestReport()));
  }

  private void setRunTestConfig(RunTestStep runTestStep, ConfigBuilder configBuilder) {
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
        .testReport(convertTestReport(runTestStep.getUnitTestReport()));
  }

  private TestReport convertTestReport(UnitTestReport unitTestReport) {
    if (unitTestReport == null) {
      return null;
    }

    if (unitTestReport.getType() != UnitTestReport.Type.JUNIT) {
      return null;
    }

    JunitTestReport junitTestReport = (JunitTestReport) unitTestReport;
    return TestReport.builder()
        .kind("Junit")
        .junitReport(JunitReport.builder().paths(junitTestReport.getPaths()).build())
        .build();
  }
}
