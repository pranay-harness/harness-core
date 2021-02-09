package io.harness.beans.serializer;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.RunTestsStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.product.ci.engine.proto.RunTestsStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.yaml.core.timeout.TimeoutUtils;

import com.google.inject.Inject;
import java.util.Optional;
import java.util.function.Supplier;

public class RunTestsStepProtobufSerializer implements ProtobufStepSerializer<RunTestsStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  public UnitStep serializeStep(StepElementConfig step, Integer port, String callbackId) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    RunTestsStepInfo runTestsStepInfo = (RunTestsStepInfo) ciStepInfo;

    long timeout = TimeoutUtils.getTimeoutInSeconds(step.getTimeout().getValue(), ciStepInfo.getDefaultTimeout());

    if (callbackId == null) {
      throw new CIStageExecutionException("CallbackId can not be null");
    }

    if (port == null) {
      throw new CIStageExecutionException("Port can not be null");
    }

    RunTestsStep.Builder runTestsStepBuilder = RunTestsStep.newBuilder();
    runTestsStepBuilder.setGoals(runTestsStepInfo.getGoals());
    runTestsStepBuilder.setContainerPort(port);
    runTestsStepBuilder.setLanguage(runTestsStepInfo.getLanguage());
    runTestsStepBuilder.setBuildTool(runTestsStepInfo.getBuildTool());
    runTestsStepBuilder.setContext(
        StepContext.newBuilder().setNumRetries(runTestsStepInfo.getRetry()).setExecutionTimeoutSecs(timeout).build());

    String skipCondition = SkipConditionUtils.getSkipCondition(step);
    return UnitStep.newBuilder()
        .setId(step.getIdentifier())
        .setTaskId(callbackId)
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(runTestsStepInfo.getDisplayName()).orElse(""))
        .setRunTests(runTestsStepBuilder.build())
        .setSkipCondition(Optional.ofNullable(skipCondition).orElse(""))
        .build();
  }
}
