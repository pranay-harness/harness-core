package io.harness.serializer.morphia;

import io.harness.beans.EnvFilter;
import io.harness.beans.GenericEntityFilter;
import io.harness.beans.WorkflowFilter;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class RbacCoreMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {}

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    w.put("beans.EnvFilter", EnvFilter.class);
    w.put("beans.GenericEntityFilter", GenericEntityFilter.class);
    w.put("beans.WorkflowFilter", WorkflowFilter.class);
  }
}
