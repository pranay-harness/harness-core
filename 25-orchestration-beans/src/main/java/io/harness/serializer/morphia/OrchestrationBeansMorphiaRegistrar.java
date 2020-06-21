package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.AdviserParameters;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.data.OutcomeInstance;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.facilitator.PassThroughData;
import io.harness.facilitator.modes.ExecutableResponse;
import io.harness.facilitator.modes.async.AsyncExecutableResponse;
import io.harness.facilitator.modes.chain.task.TaskChainResponse;
import io.harness.facilitator.modes.child.ChildExecutableResponse;
import io.harness.facilitator.modes.children.ChildrenExecutableResponse;
import io.harness.facilitator.modes.task.TaskExecutableResponse;
import io.harness.interrupts.Interrupt;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.references.OutcomeRefObject;
import io.harness.references.RefObject;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepTransput;
import io.harness.tasks.Task;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(NodeExecution.class);
    set.add(PlanExecution.class);
    set.add(Interrupt.class);
    set.add(OutcomeInstance.class);
    set.add(StepTransput.class);
    set.add(FacilitatorParameters.class);
    set.add(AdviserParameters.class);
    set.add(StepParameters.class);
    set.add(Outcome.class);
    set.add(RefObject.class);
    set.add(Task.class);
    set.add(ExecutableResponse.class);
    set.add(PassThroughData.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("facilitator.DefaultFacilitatorParams", DefaultFacilitatorParams.class);
    h.put("references.OutcomeRefObject", OutcomeRefObject.class);

    h.put("facilitator.modes.async.AsyncExecutableResponse", AsyncExecutableResponse.class);
    h.put("facilitator.modes.chain.TaskChainResponse", TaskChainResponse.class);
    h.put("facilitator.modes.child.ChildExecutableResponse", ChildExecutableResponse.class);
    h.put("facilitator.modes.children.ChildrenExecutableResponse", ChildrenExecutableResponse.class);
    h.put("facilitator.modes.task.TaskExecutableResponse", TaskExecutableResponse.class);
  }
}
