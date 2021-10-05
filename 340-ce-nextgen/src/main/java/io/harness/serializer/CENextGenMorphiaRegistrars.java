package io.harness.serializer;

import io.harness.ccm.views.businessMapping.entities.BusinessMapping;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class CENextGenMorphiaRegistrars implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    // No class to register
    set.add(BusinessMapping.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // No class to register
  }
}
