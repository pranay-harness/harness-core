package io.harness.serializer.morphia;

import io.harness.beans.DelegateTask;
import io.harness.beans.SweepingOutput;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.state.inspection.ExpressionVariableUsage;
import io.harness.state.inspection.StateInspection;
import io.harness.waiter.ErrorNotifyResponseData;
import io.harness.waiter.ListNotifyResponseData;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyResponse;
import io.harness.waiter.WaitInstance;
import io.harness.waiter.WaitInstanceError;
import io.harness.waiter.WaitQueue;

import java.util.Map;
import java.util.Set;

public class OrchestrationMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(DelegateTask.class);
    set.add(NotifyEvent.class);
    set.add(NotifyResponse.class);
    set.add(StateInspection.class);
    set.add(SweepingOutput.class);
    set.add(WaitInstance.class);
    set.add(WaitInstanceError.class);
    set.add(WaitQueue.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> {
      map.put(pkgHarness + name, clazz);
    };

    h.put("waiter.ListNotifyResponseData", ListNotifyResponseData.class);
    h.put("waiter.ErrorNotifyResponseData", ErrorNotifyResponseData.class);
    h.put("state.inspection.ExpressionVariableUsage", ExpressionVariableUsage.class);
  }
}
