package io.harness.beans.serializer;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.product.ci.engine.proto.RunStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.yaml.core.StepElement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.codec.binary.Base64;

@Singleton
public class RunStepProtobufSerializer implements ProtobufStepSerializer<RunStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Override
  public String serializeToBase64(StepElement object) {
    return Base64.encodeBase64String(serializeStep(object).toByteArray());
  }

  public UnitStep serializeStep(StepElement step) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    RunStepInfo runStepInfo = (RunStepInfo) ciStepInfo;

    RunStep.Builder runStepBuilder = RunStep.newBuilder();
    runStepBuilder.addAllCommands(runStepInfo.getCommand());
    runStepBuilder.setContainerPort(runStepInfo.getPort());
    if (runStepInfo.getOutput() != null) {
      runStepBuilder.addAllEnvVarOutputs(runStepInfo.getOutput());
    }
    runStepBuilder.setContext(StepContext.newBuilder()
                                  .setNumRetries(runStepInfo.getRetry())
                                  .setExecutionTimeoutSecs(runStepInfo.getTimeout())
                                  .build());

    String skipCondition = SkipConditionUtils.getSkipCondition(step);
    return UnitStep.newBuilder()
        .setId(runStepInfo.getIdentifier())
        .setTaskId(runStepInfo.getCallbackId())
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(runStepInfo.getDisplayName()).orElse(""))
        .setRun(runStepBuilder.build())
        .setSkipCondition(Optional.ofNullable(skipCondition).orElse(""))
        .build();
  }
}
