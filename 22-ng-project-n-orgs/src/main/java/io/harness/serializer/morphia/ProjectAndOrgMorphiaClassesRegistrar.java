package io.harness.serializer.morphia;

import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.core.NGAccountAccess;
import io.harness.ng.core.OrganizationAccess;
import io.harness.ng.core.ProjectAccess;
import io.harness.ng.core.entities.Organization;
import io.harness.ng.core.entities.Project;

import java.util.Set;

public class ProjectAndOrgMorphiaClassesRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(Project.class);
    set.add(Organization.class);
    set.add(ProjectAccess.class);
    set.add(OrganizationAccess.class);
    set.add(NGAccountAccess.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // Nothing to register
  }
}
