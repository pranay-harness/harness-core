package io.harness.serializer.morphia;

import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;

import java.util.Set;

public class CapabilityMorphiaClassesRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(CapabilityRequirement.class);
    set.add(CapabilitySubjectPermission.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
