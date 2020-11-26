package io.harness.beans.serializer;

import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.product.ci.engine.proto.PluginStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.function.Supplier;
import org.apache.commons.codec.binary.Base64;

@Singleton
public class PluginStepProtobufSerializer implements ProtobufSerializer<PluginStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Override
  public String serialize(PluginStepInfo object) {
    return Base64.encodeBase64String(convertPluginStepInfo(object).toByteArray());
  }

  public UnitStep convertPluginStepInfo(PluginStepInfo pluginStepInfo) {
    StepContext stepContext = StepContext.newBuilder()
                                  .setNumRetries(pluginStepInfo.getRetry())
                                  .setExecutionTimeoutSecs(pluginStepInfo.getTimeout())
                                  .build();
    PluginStep pluginStep = PluginStep.newBuilder()
                                .setContainerPort(pluginStepInfo.getPort())
                                .setImage(pluginStepInfo.getImage())
                                .setContext(stepContext)
                                .build();
    return UnitStep.newBuilder()
        .setId(pluginStepInfo.getIdentifier())
        .setTaskId(pluginStepInfo.getCallbackId())
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(pluginStepInfo.getDisplayName()).orElse(""))
        .setSkipCondition(Optional.ofNullable(pluginStepInfo.getSkipCondition()).orElse(""))
        .setPlugin(pluginStep)
        .build();
  }
}
