package io.harness.beans.serializer;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.callback.DelegateCallbackToken;
import io.harness.product.ci.engine.proto.PluginStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;

import com.google.inject.Inject;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.codec.binary.Base64;

public class PluginCompatibleStepSerializer implements ProtobufStepSerializer<PluginCompatibleStep> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Override
  public UnitStep serializeStep(PluginCompatibleStep step) {
    StepContext stepContext =
        StepContext.newBuilder().setNumRetries(step.getRetry()).setExecutionTimeoutSecs(step.getTimeout()).build();
    PluginStep pluginStep = PluginStep.newBuilder()
                                .setContainerPort(step.getPort())
                                .setImage(step.getImage())
                                .setContext(stepContext)
                                .build();
    return UnitStep.newBuilder()
        .setId(step.getIdentifier())
        .setTaskId(step.getCallbackId())
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(step.getDisplayName()).orElse(""))
        .setPlugin(pluginStep)
        .build();
  }

  @Override
  public String serializeToBase64(PluginCompatibleStep step) {
    return Base64.encodeBase64String(serializeStep(step).toByteArray());
  }
}
