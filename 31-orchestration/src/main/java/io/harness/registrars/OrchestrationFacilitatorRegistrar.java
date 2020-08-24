package io.harness.registrars;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.facilitator.Facilitator;
import io.harness.facilitator.FacilitatorType;
import io.harness.facilitator.async.AsyncFacilitator;
import io.harness.facilitator.chain.ChildChainFacilitator;
import io.harness.facilitator.chain.TaskChainFacilitator;
import io.harness.facilitator.chain.TaskChainV2Facilitator;
import io.harness.facilitator.chain.TaskChainV3Facilitator;
import io.harness.facilitator.child.ChildFacilitator;
import io.harness.facilitator.children.ChildrenFacilitator;
import io.harness.facilitator.sync.SyncFacilitator;
import io.harness.facilitator.task.TaskFacilitator;
import io.harness.facilitator.taskv2.TaskV2Facilitator;
import io.harness.facilitator.taskv3.TaskV3Facilitator;
import io.harness.registries.registrar.FacilitatorRegistrar;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Set;

@OwnedBy(CDC)
public class OrchestrationFacilitatorRegistrar implements FacilitatorRegistrar {
  @Override
  public void register(Set<Pair<FacilitatorType, Class<? extends Facilitator>>> facilitatorClasses) {
    facilitatorClasses.add(Pair.of(AsyncFacilitator.FACILITATOR_TYPE, AsyncFacilitator.class));
    facilitatorClasses.add(Pair.of(SyncFacilitator.FACILITATOR_TYPE, SyncFacilitator.class));
    facilitatorClasses.add(Pair.of(ChildFacilitator.FACILITATOR_TYPE, ChildFacilitator.class));
    facilitatorClasses.add(Pair.of(ChildrenFacilitator.FACILITATOR_TYPE, ChildrenFacilitator.class));
    facilitatorClasses.add(Pair.of(TaskFacilitator.FACILITATOR_TYPE, TaskFacilitator.class));
    facilitatorClasses.add(Pair.of(TaskChainFacilitator.FACILITATOR_TYPE, TaskChainFacilitator.class));
    facilitatorClasses.add(Pair.of(ChildChainFacilitator.FACILITATOR_TYPE, ChildChainFacilitator.class));
    facilitatorClasses.add(Pair.of(TaskV2Facilitator.FACILITATOR_TYPE, TaskV2Facilitator.class));
    facilitatorClasses.add(Pair.of(TaskChainV2Facilitator.FACILITATOR_TYPE, TaskChainV2Facilitator.class));
    facilitatorClasses.add(Pair.of(TaskV3Facilitator.FACILITATOR_TYPE, TaskV3Facilitator.class));
    facilitatorClasses.add(Pair.of(TaskChainV3Facilitator.FACILITATOR_TYPE, TaskChainV3Facilitator.class));
  }
}
