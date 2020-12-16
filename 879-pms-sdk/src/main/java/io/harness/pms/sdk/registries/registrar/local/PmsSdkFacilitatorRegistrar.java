package io.harness.pms.sdk.registries.registrar.local;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.facilitator.Facilitator;
import io.harness.pms.sdk.core.facilitator.async.AsyncFacilitator;
import io.harness.pms.sdk.core.facilitator.chain.ChildChainFacilitator;
import io.harness.pms.sdk.core.facilitator.chain.TaskChainFacilitator;
import io.harness.pms.sdk.core.facilitator.child.ChildFacilitator;
import io.harness.pms.sdk.core.facilitator.chilidren.ChildrenFacilitator;
import io.harness.pms.sdk.core.facilitator.sync.SyncFacilitator;
import io.harness.pms.sdk.core.facilitator.task.TaskFacilitator;
import io.harness.pms.sdk.registries.registrar.FacilitatorRegistrar;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDC)
public class PmsSdkFacilitatorRegistrar implements FacilitatorRegistrar {
  @Inject private Injector injector;

  @Override
  public void register(Set<Pair<FacilitatorType, Facilitator>> facilitatorClasses) {
    facilitatorClasses.add(Pair.of(AsyncFacilitator.FACILITATOR_TYPE, injector.getInstance(AsyncFacilitator.class)));
    facilitatorClasses.add(Pair.of(SyncFacilitator.FACILITATOR_TYPE, injector.getInstance(SyncFacilitator.class)));
    facilitatorClasses.add(Pair.of(ChildFacilitator.FACILITATOR_TYPE, injector.getInstance(ChildFacilitator.class)));
    facilitatorClasses.add(
        Pair.of(ChildrenFacilitator.FACILITATOR_TYPE, injector.getInstance(ChildrenFacilitator.class)));
    facilitatorClasses.add(Pair.of(TaskFacilitator.FACILITATOR_TYPE, injector.getInstance(TaskFacilitator.class)));
    facilitatorClasses.add(
        Pair.of(TaskChainFacilitator.FACILITATOR_TYPE, injector.getInstance(TaskChainFacilitator.class)));
    facilitatorClasses.add(
        Pair.of(ChildChainFacilitator.FACILITATOR_TYPE, injector.getInstance(ChildChainFacilitator.class)));
  }
}
