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

import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class PmsSdkFacilitatorRegistrar {
  public Map<FacilitatorType, Class<? extends Facilitator>> getEngineFacilitators() {
    Map<FacilitatorType, Class<? extends Facilitator>> engineFacilitators = new HashMap<>();

    engineFacilitators.put(AsyncFacilitator.FACILITATOR_TYPE, AsyncFacilitator.class);
    engineFacilitators.put(SyncFacilitator.FACILITATOR_TYPE, SyncFacilitator.class);
    engineFacilitators.put(ChildFacilitator.FACILITATOR_TYPE, ChildFacilitator.class);
    engineFacilitators.put(ChildrenFacilitator.FACILITATOR_TYPE, ChildrenFacilitator.class);
    engineFacilitators.put(TaskFacilitator.FACILITATOR_TYPE, TaskFacilitator.class);
    engineFacilitators.put(TaskChainFacilitator.FACILITATOR_TYPE, TaskChainFacilitator.class);
    engineFacilitators.put(ChildChainFacilitator.FACILITATOR_TYPE, ChildChainFacilitator.class);

    return engineFacilitators;
  }
}
