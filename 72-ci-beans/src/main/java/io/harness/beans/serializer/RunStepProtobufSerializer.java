package io.harness.beans.serializer;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.product.ci.engine.proto.RunStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;
import org.apache.commons.codec.binary.Base64;

import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class RunStepProtobufSerializer implements ProtobufSerializer<RunStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Override
  public String serialize(RunStepInfo object) {
    return Base64.encodeBase64String(convertRunStepInfo(object).toByteArray());
  }

  public UnitStep convertRunStepInfo(RunStepInfo runStepInfo) {
    RunStep.Builder runStepBuilder = RunStep.newBuilder();
    runStepBuilder.addAllCommands(runStepInfo.getCommand());
    if (runStepInfo.getOutput() != null) {
      runStepBuilder.addAllEnvVarOutputs(runStepInfo.getOutput());
    }
    runStepBuilder.setContext(StepContext.newBuilder()
                                  .setNumRetries(runStepInfo.getRetry())
                                  .setExecutionTimeoutSecs(runStepInfo.getTimeout())
                                  .build());
    return UnitStep.newBuilder()
        .setId(runStepInfo.getIdentifier())
        .setTaskId(runStepInfo.getCallbackId())
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(runStepInfo.getDisplayName()).orElse(""))
        .setRun(runStepBuilder.build())
        .build();
  }
}
