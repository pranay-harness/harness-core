package io.harness.serializer.morphia;

import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
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
  }
}
