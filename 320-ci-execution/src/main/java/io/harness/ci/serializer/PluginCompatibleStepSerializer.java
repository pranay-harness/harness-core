package io.harness.ci.serializer;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.common.CIExecutionConstants.PLUGIN_ARTIFACT_FILE_VALUE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoUtils;
import io.harness.callback.DelegateCallbackToken;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.plancreator.steps.StepElementConfig;
import io.harness.pms.yaml.ParameterField;
import io.harness.product.ci.engine.proto.PluginStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.stateutils.buildstate.PluginSettingUtils;
import io.harness.yaml.core.timeout.Timeout;
import io.harness.yaml.core.timeout.TimeoutUtils;

import com.google.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@OwnedBy(CI)
public class PluginCompatibleStepSerializer implements ProtobufStepSerializer<PluginCompatibleStep> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;
  @Inject private CIExecutionServiceConfig ciExecutionServiceConfig;

  @Override
  public UnitStep serializeStep(StepElementConfig step, Integer port, String callbackId, String logKey) {
    CIStepInfo ciStepInfo = (CIStepInfo) step.getStepSpecType();
    PluginCompatibleStep pluginCompatibleStep = (PluginCompatibleStep) ciStepInfo;

    long timeout = TimeoutUtils.getTimeoutInSeconds(step.getTimeout(), ciStepInfo.getDefaultTimeout());
    StepContext stepContext = StepContext.newBuilder()
                                  .setNumRetries(pluginCompatibleStep.getRetry())
                                  .setExecutionTimeoutSecs(timeout)
                                  .build();
    if (port == null) {
      throw new CIStageExecutionException("Port can not be null");
    }

    if (callbackId == null) {
      throw new CIStageExecutionException("callbackId can not be null");
    }

    PluginStep pluginStep =
        PluginStep.newBuilder()
            .setContainerPort(port)
            .setImage(CIStepInfoUtils.getPluginCustomStepImage(pluginCompatibleStep, ciExecutionServiceConfig))
            .addAllEntrypoint(
                CIStepInfoUtils.getPluginCustomStepEntrypoint(pluginCompatibleStep, ciExecutionServiceConfig))
            .setContext(stepContext)
            .setArtifactFilePath(PLUGIN_ARTIFACT_FILE_VALUE)
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

  public UnitStep serializeStepWithStepParameters(PluginCompatibleStep pluginCompatibleStep, Integer port,
      String callbackId, String logKey, String identifier, ParameterField<Timeout> parameterFieldTimeout,
      String accountId, String stepName) {
    if (port == null) {
      throw new CIStageExecutionException("Port can not be null");
    }

    if (callbackId == null) {
      throw new CIStageExecutionException("callbackId can not be null");
    }

    long timeout = TimeoutUtils.getTimeoutInSeconds(parameterFieldTimeout, pluginCompatibleStep.getDefaultTimeout());

    StepContext stepContext = StepContext.newBuilder().setExecutionTimeoutSecs(timeout).build();
    Map<String, String> envVarMap =
        PluginSettingUtils.getPluginCompatibleEnvVariables(pluginCompatibleStep, identifier, timeout);
    PluginStep pluginStep =
        PluginStep.newBuilder()
            .setContainerPort(port)
            .setImage(CIStepInfoUtils.getPluginCustomStepImage(pluginCompatibleStep, ciExecutionServiceConfig))
            .addAllEntrypoint(
                CIStepInfoUtils.getPluginCustomStepEntrypoint(pluginCompatibleStep, ciExecutionServiceConfig))
            .setContext(stepContext)
            .putAllEnvironment(envVarMap)
            .setArtifactFilePath(PLUGIN_ARTIFACT_FILE_VALUE)
            .build();

    return UnitStep.newBuilder()
        .setAccountId(accountId)
        .setContainerPort(port)
        .setId(identifier)
        .setTaskId(callbackId)
        .setCallbackToken(delegateCallbackTokenSupplier.get().getToken())
        .setDisplayName(stepName)
        .setPlugin(pluginStep)
        .setLogKey(logKey)
        .build();
  }
}
