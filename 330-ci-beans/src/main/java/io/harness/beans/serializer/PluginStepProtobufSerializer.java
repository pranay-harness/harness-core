package io.harness.beans.serializer;

import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.product.ci.engine.proto.PluginStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.codec.binary.Base64;

@Singleton
public class PluginStepProtobufSerializer implements ProtobufStepSerializer<PluginStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Override
  public String serializeToBase64(StepElementConfig step) {
    return Base64.encodeBase64String(serializeStep(step).toByteArray());
  }

  public UnitStep serializeStep(StepElementConfig step) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    PluginStepInfo pluginStepInfo = (PluginStepInfo) ciStepInfo;

    StepContext stepContext = StepContext.newBuilder()
                                  .setNumRetries(pluginStepInfo.getRetry())
                                  .setExecutionTimeoutSecs(pluginStepInfo.getTimeout())
                                  .build();
    PluginStep pluginStep = PluginStep.newBuilder()
                                .setContainerPort(pluginStepInfo.getPort())
                                .setImage(pluginStepInfo.getImage())
                                .setContext(stepContext)
                                .build();

    String skipCondition = SkipConditionUtils.getSkipCondition(step);
    return UnitStep.newBuilder()
        .setId(step.getIdentifier())
        .setTaskId(pluginStepInfo.getCallbackId())
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(step.getName()).orElse(""))
        .setSkipCondition(Optional.ofNullable(skipCondition).orElse(""))
        .setPlugin(pluginStep)
        .build();
  }
}
