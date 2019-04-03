package io.harness;

import com.google.common.collect.ImmutableSet;

import io.harness.beans.DelegateTask;
import io.harness.beans.SweepingOutput;
import io.harness.state.inspection.StateInspection;
import io.harness.waiter.NotifyEvent;
import io.harness.waiter.NotifyResponse;
import io.harness.waiter.WaitInstance;
import io.harness.waiter.WaitInstanceError;
import io.harness.waiter.WaitQueue;

import java.util.Set;

public class OrchestrationMorphiaClasses {
  public static final Set<Class> classes =
      ImmutableSet.<Class>of(DelegateTask.class, NotifyEvent.class, NotifyResponse.class, StateInspection.class,
          SweepingOutput.class, WaitInstance.class, WaitInstanceError.class, WaitQueue.class);
}
