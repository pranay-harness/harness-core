package io.harness.serializer.morphia;

import io.harness.advisers.ignore.IgnoreAdviserParameters;
import io.harness.advisers.retry.RetryAdviserParameters;
import io.harness.advisers.success.OnSuccessAdviserParameters;
import io.harness.beans.DelegateTask;
import io.harness.beans.SweepingOutput;
import io.harness.beans.SweepingOutputInstance;
import io.harness.delay.DelayEvent;
import io.harness.delay.DelayEventNotifyData;
import io.harness.engine.resume.EngineResumeAllCallback;
import io.harness.engine.resume.EngineResumeCallback;
import io.harness.engine.resume.EngineWaitResumeCallback;
import io.harness.engine.resume.EngineWaitRetryCallback;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.state.core.dummy.DummySectionOutcome;
import io.harness.state.core.dummy.DummySectionStepParameters;
import io.harness.state.core.fork.ForkStepParameters;
import io.harness.state.core.section.SectionStepParameters;
import io.harness.state.core.section.chain.SectionChainPassThroughData;
import io.harness.state.core.section.chain.SectionChainStepParameters;
import io.harness.state.inspection.ExpressionVariableUsage;
import io.harness.state.inspection.StateInspection;
import io.harness.waiter.ListNotifyResponseData;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyResponse;
import io.harness.waiter.WaitInstance;

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
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("state.inspection.ExpressionVariableUsage", ExpressionVariableUsage.class);
    h.put("waiter.ListNotifyResponseData", ListNotifyResponseData.class);

    // Engine Callback
    h.put("engine.resume.EngineResumeAllCallback", EngineResumeAllCallback.class);
    h.put("engine.resume.EngineResumeCallback", EngineResumeCallback.class);
    h.put("engine.resume.EngineWaitResumeCallback", EngineWaitResumeCallback.class);
    h.put("engine.resume.EngineWaitRetryCallback", EngineWaitRetryCallback.class);

    // Adviser Related Classes
    h.put("adviser.impl.ignore.IgnoreAdviserParameters", IgnoreAdviserParameters.class);
    h.put("adviser.impl.retry.RetryAdviserParameters", RetryAdviserParameters.class);
    h.put("adviser.impl.success.OnSuccessAdviserParameters", OnSuccessAdviserParameters.class);

    // Facilitator related classes
    h.put("delay.DelayEventNotifyData", DelayEventNotifyData.class);

    // State Related Classes
    h.put("state.core.dummy.DummySectionOutcome", DummySectionOutcome.class);
    h.put("state.core.dummy.DummySectionStepParameters", DummySectionStepParameters.class);
    h.put("state.core.fork.ForkStepParameters", ForkStepParameters.class);
    h.put("state.core.section.chain.SectionChainPassThroughData", SectionChainPassThroughData.class);
    h.put("state.core.section.chain.SectionStepParameters", SectionChainStepParameters.class);
    h.put("state.core.section.SectionStepParameters", SectionStepParameters.class);
  }
}