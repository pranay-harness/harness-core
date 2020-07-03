package io.harness.serializer.spring;

import io.harness.adviser.AdviserType;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.barriers.BarrierNode;
import io.harness.data.ExecutionSweepingOutputInstance;
import io.harness.data.OutcomeInstance;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.FacilitatorType;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.facilitator.modes.chain.child.ChildChainResponse;
import io.harness.facilitator.modes.chain.task.TaskChainExecutableResponse;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.task.TaskExecutableResponse;
import io.harness.interrupts.Interrupt;
import io.harness.interrupts.InterruptEffect;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.references.OutcomeRefObject;
import io.harness.references.SweepingOutputRefObject;
import io.harness.spring.AliasRegistrar;
import io.harness.state.StepType;
import io.harness.state.io.FailureInfo;

import java.util.Map;

public class OrchestrationBeansAliasRegistrar implements AliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("ambiance", Ambiance.class);
    orchestrationElements.put("level", Level.class);
    orchestrationElements.put("planNode", PlanNode.class);
    orchestrationElements.put("plan", Plan.class);
    orchestrationElements.put("stepType", StepType.class);
    orchestrationElements.put("facilitatorType", FacilitatorType.class);
    orchestrationElements.put("adviserType", AdviserType.class);
    orchestrationElements.put("asyncExecutableResponse", AsyncExecutableResponse.class);
    orchestrationElements.put("barrierNode", BarrierNode.class);
    orchestrationElements.put("childChainResponse", ChildChainResponse.class);
    orchestrationElements.put("childExecutableResponse", ChildExecutableResponse.class);
    orchestrationElements.put("childrenExecutableResponse", ChildrenExecutableResponse.class);
    orchestrationElements.put("defaultFacilitatorParams", DefaultFacilitatorParams.class);
    orchestrationElements.put("failureInfo", FailureInfo.class);
    orchestrationElements.put("interrupt", Interrupt.class);
    orchestrationElements.put("interruptEffect", InterruptEffect.class);
    orchestrationElements.put("nodeExecution", NodeExecution.class);
    orchestrationElements.put("outcomeInstance", OutcomeInstance.class);
    orchestrationElements.put("outcomeRefObject", OutcomeRefObject.class);
    orchestrationElements.put("planExecution", PlanExecution.class);
    orchestrationElements.put("sweepingOutputRefObject", SweepingOutputRefObject.class);
    orchestrationElements.put("taskChainExecutableResponse", TaskChainExecutableResponse.class);
    orchestrationElements.put("taskExecutableResponse", TaskExecutableResponse.class);
    orchestrationElements.put("executionSweepingOutput", ExecutionSweepingOutputInstance.class);
  }
}
