package io.harness.beans.serializer;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.product.ci.engine.proto.PluginStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.yaml.core.StepElement;

import com.google.inject.Inject;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.codec.binary.Base64;

public class PluginCompatibleStepSerializer implements ProtobufStepSerializer<PluginCompatibleStep> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Override
  public UnitStep serializeStep(StepElement step) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    PluginCompatibleStep pluginCompatibleStep = (PluginCompatibleStep) ciStepInfo;

    StepContext stepContext = StepContext.newBuilder()
                                  .setNumRetries(pluginCompatibleStep.getRetry())
                                  .setExecutionTimeoutSecs(pluginCompatibleStep.getTimeout())
                                  .build();
    PluginStep pluginStep = PluginStep.newBuilder()
                                .setContainerPort(pluginCompatibleStep.getPort())
                                .setImage(pluginCompatibleStep.getImage())
                                .setContext(stepContext)
                                .build();

    String skipCondition = SkipConditionUtils.getSkipCondition(step);
    return UnitStep.newBuilder()
        .setId(step.getIdentifier())
        .setTaskId(pluginCompatibleStep.getCallbackId())
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(pluginCompatibleStep.getDisplayName()).orElse(""))
        .setSkipCondition(Optional.ofNullable(skipCondition).orElse(""))
        .setPlugin(pluginStep)
        .build();
  }

  @Override
  public String serializeToBase64(StepElement step) {
    return Base64.encodeBase64String(serializeStep(step).toByteArray());
  }
}
