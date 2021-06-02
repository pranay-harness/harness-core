package io.harness.serializer.morphia;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.execution.SdkResponseEvent;
import io.harness.pms.execution.facilitator.DefaultFacilitatorParams;
import io.harness.pms.interrupts.InterruptEvent;

import java.util.Set;

@OwnedBy(CDC)
public class PmsCommonsMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(NodeExecutionEvent.class);
    set.add(InterruptEvent.class);
    set.add(SdkResponseEvent.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    h.put("facilitator.DefaultFacilitatorParams", DefaultFacilitatorParams.class);
  }
}
