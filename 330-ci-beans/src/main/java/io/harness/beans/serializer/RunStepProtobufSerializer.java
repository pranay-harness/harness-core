package io.harness.beans.serializer;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.yaml.extended.reports.JUnitTestReport;
import io.harness.beans.yaml.extended.reports.UnitTestReport;
import io.harness.beans.yaml.extended.reports.UnitTestReportType;
import io.harness.callback.DelegateCallbackToken;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.product.ci.engine.proto.Report;
import io.harness.product.ci.engine.proto.RunStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.core.timeout.TimeoutUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class RunStepProtobufSerializer implements ProtobufStepSerializer<RunStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  public UnitStep serializeStep(StepElementConfig step, Integer port, String callbackId, String logKey) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    RunStepInfo runStepInfo = (RunStepInfo) ciStepInfo;

    if (callbackId == null) {
      throw new CIStageExecutionException("CallbackId can not be null");
    }

    RunStep.Builder runStepBuilder = RunStep.newBuilder();
    runStepBuilder.setCommand(RunTimeInputHandler.resolveStringParameter(
        "Command", "Run", step.getIdentifier(), runStepInfo.getCommand(), true));
    if (port == null) {
      throw new CIStageExecutionException("Port can not be null");
    }
    runStepBuilder.setContainerPort(port);

    UnitTestReport reports = runStepInfo.getReports();
    if (reports != null) {
      if (reports.getType() == UnitTestReportType.JUNIT) {
        JUnitTestReport junitTestReport = (JUnitTestReport) reports.getSpec();
        List<String> resolvedReport = junitTestReport.resolve(step.getIdentifier(), "run");

        Report report = Report.newBuilder().setType(Report.Type.JUNIT).addAllPaths(resolvedReport).build();
        runStepBuilder.addReports(report);
      }
    }

    List<String> output = RunTimeInputHandler.resolveListParameter(
        "OutputVariables", "Run", step.getIdentifier(), runStepInfo.getOutputVariables(), false);
    if (isNotEmpty(output)) {
      runStepBuilder.addAllEnvVarOutputs(output);
    }

    long timeout = TimeoutUtils.getTimeoutInSeconds(step.getTimeout(), ciStepInfo.getDefaultTimeout());
    runStepBuilder.setContext(
        StepContext.newBuilder().setNumRetries(runStepInfo.getRetry()).setExecutionTimeoutSecs(timeout).build());

    String skipCondition = SkipConditionUtils.getSkipCondition(step);
    return UnitStep.newBuilder()
        .setId(step.getIdentifier())
        .setTaskId(callbackId)
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(runStepInfo.getDisplayName()).orElse(""))
        .setRun(runStepBuilder.build())
        .setSkipCondition(Optional.ofNullable(skipCondition).orElse(""))
        .setLogKey(logKey)
        .build();
  }

  public UnitStep serializeStepWithStepParameters(RunStepInfo runStepInfo, Integer port, String callbackId,
      String logKey, String identifier, ParameterField<Timeout> parameterFieldTimeout, String accountId,
      String stepName) {
    if (callbackId == null) {
      throw new CIStageExecutionException("CallbackId can not be null");
    }

    if (port == null) {
      throw new CIStageExecutionException("Port can not be null");
    }

    RunStep.Builder runStepBuilder = RunStep.newBuilder();
    runStepBuilder.setCommand(
        RunTimeInputHandler.resolveStringParameter("Command", "Run", identifier, runStepInfo.getCommand(), true));

    runStepBuilder.setContainerPort(port);

    UnitTestReport reports = runStepInfo.getReports();
    if (reports != null) {
      if (reports.getType() == UnitTestReportType.JUNIT) {
        JUnitTestReport junitTestReport = (JUnitTestReport) reports.getSpec();
        List<String> resolvedReport = junitTestReport.resolve(identifier, "run");

        Report report = Report.newBuilder().setType(Report.Type.JUNIT).addAllPaths(resolvedReport).build();
        runStepBuilder.addReports(report);
      }
    }

    List<String> output = RunTimeInputHandler.resolveListParameter(
        "OutputVariables", "Run", identifier, runStepInfo.getOutputVariables(), false);
    if (isNotEmpty(output)) {
      runStepBuilder.addAllEnvVarOutputs(output);
    }

    long timeout = TimeoutUtils.getTimeoutInSeconds(parameterFieldTimeout, runStepInfo.getDefaultTimeout());
    runStepBuilder.setContext(StepContext.newBuilder().setExecutionTimeoutSecs(timeout).build());

    return UnitStep.newBuilder()
        .setAccountId(accountId)
        .setContainerPort(port)
        .setId(identifier)
        .setTaskId(callbackId)
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(stepName)
        .setRun(runStepBuilder.build())
        .setLogKey(logKey)
        .build();
  }
}
