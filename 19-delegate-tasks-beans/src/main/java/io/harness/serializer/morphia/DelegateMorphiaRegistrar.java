package io.harness.serializer.morphia;

import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.executioncapability.AlwaysFalseValidationCapability;
import io.harness.delegate.beans.executioncapability.AwsRegionCapability;
import io.harness.delegate.beans.executioncapability.ChartMuseumCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;
import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.task.spotinst.request.SpotInstDeployTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstSetupTaskParameters;
import io.harness.delegate.task.spotinst.request.SpotInstSwapRoutesTaskParameters;
import io.harness.morphia.MorphiaRegistrar;

import java.util.Map;
import java.util.Set;

public class DelegateMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(ExecutionCapabilityDemander.class);
    set.add(ExecutionCapability.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> {
      map.put(PKG_HARNESS + name, clazz);
    };

    h.put("delegate.command.CommandExecutionResult", CommandExecutionResult.class);
    h.put("delegate.beans.ErrorNotifyResponseData", ErrorNotifyResponseData.class);
    h.put("delegate.task.spotinst.request.SpotInstDeployTaskParameters", SpotInstDeployTaskParameters.class);
    h.put("delegate.task.spotinst.request.SpotInstSetupTaskParameters", SpotInstSetupTaskParameters.class);
    h.put("delegate.task.spotinst.request.SpotInstSetupTaskParameters", SpotInstSwapRoutesTaskParameters.class);

    h.put("waiter.ErrorNotifyResponseData", ErrorNotifyResponseData.class);

    h.put("delegate.beans.executioncapability.AlwaysFalseValidationCapability", AlwaysFalseValidationCapability.class);
    h.put("delegate.beans.executioncapability.AwsRegionCapability", AwsRegionCapability.class);
    h.put("delegate.beans.executioncapability.ChartMuseumCapability", ChartMuseumCapability.class);
    h.put("delegate.beans.executioncapability.HttpConnectionExecutionCapability",
        HttpConnectionExecutionCapability.class);
    h.put("delegate.beans.executioncapability.ProcessExecutorCapability", ProcessExecutorCapability.class);
    h.put("delegate.beans.executioncapability.SocketConnectivityExecutionCapability",
        SocketConnectivityExecutionCapability.class);
    h.put("delegate.beans.executioncapability.SystemEnvCheckerCapability", SystemEnvCheckerCapability.class);
  }
}
