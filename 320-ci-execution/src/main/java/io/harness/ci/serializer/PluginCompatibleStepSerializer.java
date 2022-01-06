/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ci.serializer;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.common.CIExecutionConstants.PLUGIN_ARTIFACT_FILE_VALUE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfoUtils;
import io.harness.beans.sweepingoutputs.StageInfraDetails.Type;
import io.harness.callback.DelegateCallbackToken;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.exception.ngexception.CIStageExecutionException;
import io.harness.pms.yaml.ParameterField;
import io.harness.product.ci.engine.proto.PluginStep;
import io.harness.product.ci.engine.proto.StepContext;
import io.harness.product.ci.engine.proto.UnitStep;
import io.harness.stateutils.buildstate.PluginSettingUtils;
import io.harness.utils.TimeoutUtils;
import io.harness.yaml.core.timeout.Timeout;

import com.google.inject.Inject;
import java.util.Map;
import java.util.function.Supplier;

@OwnedBy(CI)
public class PluginCompatibleStepSerializer implements ProtobufStepSerializer<PluginCompatibleStep> {
  @Inject private Supplier<DelegateCallbackToken> delegateCallbackTokenSupplier;
  @Inject private CIExecutionServiceConfig ciExecutionServiceConfig;

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
        PluginSettingUtils.getPluginCompatibleEnvVariables(pluginCompatibleStep, identifier, timeout, Type.K8);
    PluginStep pluginStep =
        PluginStep.newBuilder()
            .setContainerPort(port)
            .setImage(CIStepInfoUtils.getPluginCustomStepImage(pluginCompatibleStep, ciExecutionServiceConfig, Type.K8))
            .addAllEntrypoint(
                CIStepInfoUtils.getK8PluginCustomStepEntrypoint(pluginCompatibleStep, ciExecutionServiceConfig))
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
