package io.harness.beans.serializer;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.yaml.extended.reports.JunitTestReport;
import io.harness.beans.yaml.extended.reports.UnitTestReport;
import io.harness.callback.DelegateCallbackToken;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.product.ci.engine.proto.Report;
import io.harness.product.ci.engine.proto.RunStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.yaml.core.timeout.TimeoutUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class RunStepProtobufSerializer implements ProtobufStepSerializer<RunStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  public UnitStep serializeStep(StepElementConfig step, Integer port, String callbackId) {
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

    List<UnitTestReport> reports = runStepInfo.getReports();
    if (isNotEmpty(reports)) {
      for (UnitTestReport unitTestReport : reports) {
        if (unitTestReport.getType() == UnitTestReport.Type.JUNIT) {
          Report report = Report.newBuilder()
                              .setType(Report.Type.JUNIT)
                              .addAllPaths(resolveJunitReport(unitTestReport, step.getIdentifier()))
                              .build();
          runStepBuilder.addReports(report);
        }
      }
    }

    List<String> output = RunTimeInputHandler.resolveListParameter(
        "OutputVariables", "Run", runStepInfo.getIdentifier(), runStepInfo.getOutputVariables(), false);
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
        .build();
  }

  public List<String> resolveJunitReport(UnitTestReport unitTestReport, String identifier) {
    JunitTestReport junitTestReport = (JunitTestReport) unitTestReport;
    return RunTimeInputHandler.resolveListParameter(
        "paths", "run", identifier, junitTestReport.getSpec().getPaths(), false);
  }
}
