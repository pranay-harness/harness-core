package io.harness.ci.serializer;

import static io.harness.beans.serializer.RunTimeInputHandler.resolveMapParameter;
import static io.harness.common.CIExecutionConstants.PLUGIN_ENV_PREFIX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.Collections.emptyList;

import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.callback.DelegateCallbackToken;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.product.ci.engine.proto.PluginStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.core.timeout.TimeoutUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Singleton
public class PluginStepProtobufSerializer implements ProtobufStepSerializer<PluginStepInfo> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;

  @Override

  public UnitStep serializeStep(StepElementConfig step, Integer port, String callbackId, String logKey) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    PluginStepInfo pluginStepInfo = (PluginStepInfo) ciStepInfo;

    long timeout = TimeoutUtils.getTimeoutInSeconds(step.getTimeout(), ciStepInfo.getDefaultTimeout());
    StepContext stepContext =
        StepContext.newBuilder().setNumRetries(pluginStepInfo.getRetry()).setExecutionTimeoutSecs(timeout).build();
    if (port == null) {
      throw new CIStageExecutionException("Port can not be null");
    }

    if (callbackId == null) {
      throw new CIStageExecutionException("CallbackId can not be null");
    }
    PluginStep pluginStep =
        PluginStep.newBuilder()
            .setContainerPort(port)
            .setImage(RunTimeInputHandler.resolveStringParameter(
                "Image", "Plugin", step.getIdentifier(), pluginStepInfo.getImage(), true))
            .addAllEntrypoint(Optional.ofNullable(pluginStepInfo.getEntrypoint()).orElse(emptyList()))
            .setContext(stepContext)
            .build();

    String skipCondition = SkipConditionUtils.getSkipCondition(step);
    return UnitStep.newBuilder()
        .setId(step.getIdentifier())
        .setTaskId(callbackId)
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(Optional.ofNullable(step.getName()).orElse(""))
        .setSkipCondition(Optional.ofNullable(skipCondition).orElse(""))
        .setPlugin(pluginStep)
        .setLogKey(logKey)
        .build();
  }

  public UnitStep serializeStepWithStepParameters(PluginStepInfo pluginStepInfo, Integer port, String ctrName,
      String callbackId, String logKey, String identifier, ParameterField<Timeout> parameterFieldTimeout,
      String accountId, String stepName) {
    if (callbackId == null) {
      throw new CIStageExecutionException("CallbackId can not be null");
    }

    if (port == null) {
      throw new CIStageExecutionException("Port can not be null");
    }

    long timeout = TimeoutUtils.getTimeoutInSeconds(parameterFieldTimeout, pluginStepInfo.getDefaultTimeout());
    StepContext stepContext = StepContext.newBuilder().setExecutionTimeoutSecs(timeout).build();

    Map<String, String> settings =
        resolveMapParameter("settings", "Plugin", identifier, pluginStepInfo.getSettings(), false);
    Map<String, String> envVarMap = new HashMap<>();
    if (!isEmpty(settings)) {
      for (Map.Entry<String, String> entry : settings.entrySet()) {
        String key = PLUGIN_ENV_PREFIX + entry.getKey().toUpperCase();
        envVarMap.put(key, entry.getValue());
      }
    }

    PluginStep pluginStep =
        PluginStep.newBuilder()
            .setContainerPort(port)
            .setImage(RunTimeInputHandler.resolveStringParameter(
                "Image", "Plugin", identifier, pluginStepInfo.getImage(), true))
            .addAllEntrypoint(Optional.ofNullable(pluginStepInfo.getEntrypoint()).orElse(emptyList()))
            .putAllEnvironment(envVarMap)
            .setContext(stepContext)
            .build();

    return UnitStep.newBuilder()
        .setId(identifier)
        .setTaskId(callbackId)
        .setAccountId(accountId)
        .setContainerPort(port)
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(stepName)
        .setPlugin(pluginStep)
        .setLogKey(logKey)
        .setContainerName(ctrName)
        .build();
  }
}
