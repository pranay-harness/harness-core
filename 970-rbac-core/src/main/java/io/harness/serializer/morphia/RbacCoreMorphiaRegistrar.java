package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import software.wings.security.EnvFilter;
import software.wings.security.GenericEntityFilter;
import software.wings.security.ScopedEntity;
import software.wings.security.WorkflowFilter;

import java.util.Set;

public class RbacCoreMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(ScopedEntity.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    w.put("security.EnvFilter", EnvFilter.class);
    w.put("security.GenericEntityFilter", GenericEntityFilter.class);
    w.put("security.WorkflowFilter", WorkflowFilter.class);
  }
}
