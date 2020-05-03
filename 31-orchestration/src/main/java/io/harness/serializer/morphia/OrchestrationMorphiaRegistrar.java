package io.harness.serializer.morphia;

import io.harness.adviser.impl.ignore.IgnoreAdviserParameters;
import io.harness.adviser.impl.retry.RetryAdviserParameters;
import io.harness.adviser.impl.success.OnSuccessAdviserParameters;
import io.harness.beans.DelegateTask;
import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutputInstance;
import io.harness.delay.DelayEvent;
import io.harness.delay.DelayEventNotifyData;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.engine.resume.EngineWaitResumeCallback;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.state.inspection.ExpressionVariableUsage;
import io.harness.state.inspection.StateInspection;
import io.harness.waiter.ListNotifyResponseData;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyResponse;
import io.harness.waiter.WaitInstance;

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
    set.add(SweepingOutputInstance.class);
    set.add(WaitInstance.class);
    set.add(DelayEvent.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> {
      map.put(PKG_HARNESS + name, clazz);
    };

    h.put("waiter.ListNotifyResponseData", ListNotifyResponseData.class);
    h.put("state.inspection.ExpressionVariableUsage", ExpressionVariableUsage.class);
    h.put("engine.resume.EngineResumeCallback", EngineResumeCallback.class);

    // Engine Callback
    h.put("engine.resume.EngineResumeCallback", EngineResumeCallback.class);
    h.put("engine.resume.EngineWaitResumeCallback", EngineWaitResumeCallback.class);

    // Adviser Related Classes
    h.put("adviser.impl.ignore.IgnoreAdviserParameters", IgnoreAdviserParameters.class);
    h.put("adviser.impl.retry.RetryAdviserParameters", RetryAdviserParameters.class);
    h.put("adviser.impl.success.OnSuccessAdviserParameters", OnSuccessAdviserParameters.class);

    // Facilitator related classes
    h.put("delay.DelayEventNotifyData", DelayEventNotifyData.class);
  }
}
