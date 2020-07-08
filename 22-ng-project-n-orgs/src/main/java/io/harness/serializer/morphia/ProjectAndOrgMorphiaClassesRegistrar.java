package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;

import java.util.Set;

public class ProjectAndOrgMorphiaClassesRegistrar implements io.harness.morphia.MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Project.class);
    set.add(Organization.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
