package io.harness.beans.serializer;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.yaml.extended.reports.JunitTestReport;
import io.harness.beans.yaml.extended.reports.UnitTestReport;
import io.harness.callback.DelegateCallbackToken;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.product.ci.engine.proto.Report;
import io.harness.product.ci.engine.proto.RunStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.codec.binary.Base64;

@Singleton
public class RunStepProtobufSerializer implements ProtobufStepSerializer<RunStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Override
  public String serializeToBase64(StepElementConfig object) {
    return Base64.encodeBase64String(serializeStep(object).toByteArray());
  }

  public UnitStep serializeStep(StepElementConfig step) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    RunStepInfo runStepInfo = (RunStepInfo) ciStepInfo;

    RunStep.Builder runStepBuilder = RunStep.newBuilder();
    runStepBuilder.setCommand(runStepInfo.getCommand());
    runStepBuilder.setContainerPort(runStepInfo.getPort());

    List<UnitTestReport> reports = runStepInfo.getReports();
    if (isNotEmpty(reports)) {
      for (UnitTestReport unitTestReport : reports) {
        if (unitTestReport.getType() == UnitTestReport.Type.JUNIT) {
          Report report =
              Report.newBuilder().setType(Report.Type.JUNIT).addAllPaths(resolveJunitReport(unitTestReport)).build();
          runStepBuilder.addReports(report);
        }
      }
    }

    if (runStepInfo.getOutput() != null) {
      runStepBuilder.addAllEnvVarOutputs(runStepInfo.getOutput());
    }
    runStepBuilder.setContext(StepContext.newBuilder()
                                  .setNumRetries(runStepInfo.getRetry())
                                  .setExecutionTimeoutSecs(runStepInfo.getTimeout())
                                  .build());

    String skipCondition = SkipConditionUtils.getSkipCondition(step);
    return UnitStep.newBuilder()
        .setId(step.getIdentifier())
        .setTaskId(runStepInfo.getCallbackId())
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(runStepInfo.getDisplayName()).orElse(""))
        .setRun(runStepBuilder.build())
        .setSkipCondition(Optional.ofNullable(skipCondition).orElse(""))
        .build();
  }

  public List<String> resolveJunitReport(UnitTestReport unitTestReport) {
    JunitTestReport junitTestReport = (JunitTestReport) unitTestReport;
    return junitTestReport.getSpec().getPaths();
  }
}
