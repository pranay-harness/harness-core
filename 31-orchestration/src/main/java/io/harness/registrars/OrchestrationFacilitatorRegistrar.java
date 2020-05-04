package io.harness.registrars;

import io.harness.facilitator.Facilitator;
import io.harness.facilitator.async.AsyncFacilitator;
import io.harness.facilitator.child.ChildFacilitator;
import io.harness.facilitator.children.ChildrenFacilitator;
import io.harness.facilitator.sync.SyncFacilitator;
import io.harness.registries.registrar.FacilitatorRegistrar;

import java.util.Set;

public class OrchestrationFacilitatorRegistrar implements FacilitatorRegistrar {
  @Override
  public void register(Set<Class<? extends Facilitator>> facilitatorClasses) {
    facilitatorClasses.add(AsyncFacilitator.class);
    facilitatorClasses.add(SyncFacilitator.class);
    facilitatorClasses.add(ChildFacilitator.class);
    facilitatorClasses.add(ChildrenFacilitator.class);
  }
}
