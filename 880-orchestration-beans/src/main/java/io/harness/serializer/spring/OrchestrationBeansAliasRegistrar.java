package io.harness.serializer.spring;

import io.harness.data.ExecutionSweepingOutputInstance;
import io.harness.data.OutcomeInstance;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.InterruptEffect;
import io.harness.plan.Plan;
import io.harness.spring.AliasRegistrar;
import io.harness.timeout.TimeoutDetails;
import io.harness.timeout.trackers.active.ActiveTimeoutTracker;

import java.util.Map;

public class OrchestrationBeansAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("plan", Plan.class);
    orchestrationElements.put("interrupt", Interrupt.class);
    orchestrationElements.put("interruptEffect", InterruptEffect.class);
    orchestrationElements.put("nodeExecution", NodeExecution.class);
    orchestrationElements.put("outcomeInstance", OutcomeInstance.class);
    orchestrationElements.put("planExecution", PlanExecution.class);
    orchestrationElements.put("executionSweepingOutput", ExecutionSweepingOutputInstance.class);
    orchestrationElements.put("activeTimeoutTracker", ActiveTimeoutTracker.class);
    orchestrationElements.put("timeoutDetails", TimeoutDetails.class);
    orchestrationElements.put("orchestrationEvent", OrchestrationEvent.class);
  }
}
