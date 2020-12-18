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

import com.google.inject.Injector;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;

@OwnedBy(CDC)
@UtilityClass
public class PmsSdkFacilitatorRegistrar {
  public Map<FacilitatorType, Facilitator> getEngineFacilitators(Injector injector) {
    Map<FacilitatorType, Facilitator> engineFacilitators = new HashMap<>();

    engineFacilitators.put(AsyncFacilitator.FACILITATOR_TYPE, injector.getInstance(AsyncFacilitator.class));
    engineFacilitators.put(SyncFacilitator.FACILITATOR_TYPE, injector.getInstance(SyncFacilitator.class));
    engineFacilitators.put(ChildFacilitator.FACILITATOR_TYPE, injector.getInstance(ChildFacilitator.class));
    engineFacilitators.put(ChildrenFacilitator.FACILITATOR_TYPE, injector.getInstance(ChildrenFacilitator.class));
    engineFacilitators.put(TaskFacilitator.FACILITATOR_TYPE, injector.getInstance(TaskFacilitator.class));
    engineFacilitators.put(TaskChainFacilitator.FACILITATOR_TYPE, injector.getInstance(TaskChainFacilitator.class));
    engineFacilitators.put(ChildChainFacilitator.FACILITATOR_TYPE, injector.getInstance(ChildChainFacilitator.class));

    return engineFacilitators;
  }
}
