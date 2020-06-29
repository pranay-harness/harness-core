package io.harness.serializer.spring;

import io.harness.OrchestrationBeansAliasRegistrar;
import io.harness.data.OutcomeInstance;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.facilitator.modes.chain.child.ChildChainResponse;
import io.harness.facilitator.modes.chain.task.TaskChainExecutableResponse;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.task.TaskExecutableResponse;
import io.harness.references.OutcomeRefObject;
import io.harness.references.SweepingOutputRefObject;

import java.util.Map;

public class OrchestrationBeansSpringAliasRegistrar implements OrchestrationBeansAliasRegistrar {
  @Override
  public void register(Map<String, Class<?>> orchestrationElements) {
    orchestrationElements.put("asyncExecutableResponse", AsyncExecutableResponse.class);
    orchestrationElements.put("childChainResponse", ChildChainResponse.class);
    orchestrationElements.put("childExecutableResponse", ChildExecutableResponse.class);
    orchestrationElements.put("childrenExecutableResponse", ChildrenExecutableResponse.class);
    orchestrationElements.put("defaultFacilitatorParams", DefaultFacilitatorParams.class);
    orchestrationElements.put("nodeExecutions", NodeExecution.class);
    orchestrationElements.put("outcomeInstances", OutcomeInstance.class);
    orchestrationElements.put("outcomeRefObject", OutcomeRefObject.class);
    orchestrationElements.put("planExecutions", PlanExecution.class);
    orchestrationElements.put("sweepingOutputRefObject", SweepingOutputRefObject.class);
    orchestrationElements.put("taskChainExecutableResponse", TaskChainExecutableResponse.class);
    orchestrationElements.put("taskExecutableResponse", TaskExecutableResponse.class);
  }
}
