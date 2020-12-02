package io.harness.serializer.spring;

import io.harness.data.ExecutionSweepingOutputInstance;
import io.harness.data.OutcomeInstance;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.facilitator.modes.chain.child.ChildChainResponse;
import io.harness.facilitator.modes.chain.task.TaskChainExecutableResponse;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.task.TaskExecutableResponse;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.InterruptEffect;
import io.harness.plan.Plan;
import io.harness.spring.AliasRegistrar;
import io.harness.state.io.StepOutcomeRef;
import io.harness.timeout.TimeoutDetails;
import io.harness.timeout.trackers.active.ActiveTimeoutTracker;

import java.util.Map;

public class OrchestrationBeansAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("plan", Plan.class);
    orchestrationElements.put("asyncExecutableResponse", AsyncExecutableResponse.class);
    orchestrationElements.put("childChainResponse", ChildChainResponse.class);
    orchestrationElements.put("childExecutableResponse", ChildExecutableResponse.class);
    orchestrationElements.put("childrenExecutableResponse", ChildrenExecutableResponse.class);
    orchestrationElements.put("defaultFacilitatorParams", DefaultFacilitatorParams.class);
    orchestrationElements.put("interrupt", Interrupt.class);
    orchestrationElements.put("interruptEffect", InterruptEffect.class);
    orchestrationElements.put("nodeExecution", NodeExecution.class);
    orchestrationElements.put("outcomeInstance", OutcomeInstance.class);
    orchestrationElements.put("planExecution", PlanExecution.class);
    orchestrationElements.put("taskChainExecutableResponse", TaskChainExecutableResponse.class);
    orchestrationElements.put("taskExecutableResponse", TaskExecutableResponse.class);
    orchestrationElements.put("executionSweepingOutput", ExecutionSweepingOutputInstance.class);
    orchestrationElements.put("activeTimeoutTracker", ActiveTimeoutTracker.class);
    orchestrationElements.put("stepOutcomeRef", StepOutcomeRef.class);
    orchestrationElements.put("timeoutDetails", TimeoutDetails.class);
    orchestrationElements.put("orchestrationEvent", OrchestrationEvent.class);
  }
}
