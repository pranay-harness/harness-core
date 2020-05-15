package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.adviser.AdviserParameters;
import io.harness.ambiance.dev.DefaultLevel;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.Outcome;
import io.harness.data.OutcomeInstance;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.facilitator.DefaultFacilitatorParams;
import io.harness.facilitator.FacilitatorParameters;
import io.harness.interrupts.Interrupt;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.plan.Plan;
import io.harness.references.OutcomeRefObject;
import io.harness.references.RefObject;
import io.harness.state.io.StateParameters;
import io.harness.state.io.StateTransput;

import java.util.Map;
import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationBeansMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Plan.class);
    set.add(NodeExecution.class);
    set.add(PlanExecution.class);
    set.add(Interrupt.class);
    set.add(OutcomeInstance.class);
    set.add(StateTransput.class);
    set.add(FacilitatorParameters.class);
    set.add(AdviserParameters.class);
    set.add(StateParameters.class);
    set.add(Outcome.class);
    set.add(RefObject.class);
  }

  @Override
  public void registerImplementationClasses(Map<String, Class> map) {
    final HelperPut h = (name, clazz) -> {
      map.put(PKG_HARNESS + name, clazz);
    };
    h.put("facilitator.DefaultFacilitatorParams", DefaultFacilitatorParams.class);
    h.put("ambiance.dev.DefaultLevel", DefaultLevel.class);
    h.put("references.OutcomeRefObject", OutcomeRefObject.class);
  }
}
