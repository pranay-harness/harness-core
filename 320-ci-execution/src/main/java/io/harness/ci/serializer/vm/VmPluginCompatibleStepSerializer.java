package io.harness.ci.serializer.vm;

import io.harness.beans.plugin.compatible.PluginCompatibleStep;
import io.harness.beans.steps.CIStepInfoUtils;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.EnvVariableEnum;
import io.harness.delegate.beans.ci.vm.steps.VmPluginStep;
import io.harness.ng.core.NGAccess;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.stateutils.buildstate.ConnectorUtils;
import io.harness.stateutils.buildstate.PluginSettingUtils;
import io.harness.utils.TimeoutUtils;
import io.harness.yaml.core.timeout.Timeout;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Map;

@Singleton
public class VmPluginCompatibleStepSerializer {
  @Inject private CIExecutionServiceConfig ciExecutionServiceConfig;
  @Inject private ConnectorUtils connectorUtils;

  public VmPluginStep serialize(Ambiance ambiance, PluginCompatibleStep pluginCompatibleStep, String identifier,
      ParameterField<Timeout> parameterFieldTimeout, String stepName) {
    long timeout = TimeoutUtils.getTimeoutInSeconds(parameterFieldTimeout, pluginCompatibleStep.getDefaultTimeout());
    Map<String, String> envVars =
        PluginSettingUtils.getPluginCompatibleEnvVariables(pluginCompatibleStep, identifier, timeout);
    String image = CIStepInfoUtils.getPluginCustomStepImage(pluginCompatibleStep, ciExecutionServiceConfig);

    String connectorRef = PluginSettingUtils.getConnectorRef(pluginCompatibleStep);
    NGAccess ngAccess = AmbianceUtils.getNgAccess(ambiance);
    Map<EnvVariableEnum, String> connectorSecretEnvMap =
        PluginSettingUtils.getConnectorSecretEnvMap(pluginCompatibleStep);
    ConnectorDetails connectorDetails = connectorUtils.getConnectorDetails(ngAccess, connectorRef);
    connectorDetails.setEnvToSecretsMap(connectorSecretEnvMap);

    return VmPluginStep.builder()
        .image(image)
        .connector(connectorDetails)
        .envVariables(envVars)
        .timeoutSecs(timeout)
        .build();
  }
}
