package io.harness.serializer;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.serializer.json.JsonSerializable;

import java.util.Set;

@OwnedBy(CDC)
public class PmsCommonsMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(NodeExecutionEvent.class);
    set.add(JsonSerializable.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
