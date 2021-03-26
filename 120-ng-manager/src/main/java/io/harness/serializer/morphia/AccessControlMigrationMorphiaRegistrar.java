package io.harness.serializer.morphia;

import io.harness.accesscontrol.roleassignments.api.RoleAssignmentDTO;
import io.harness.accesscontrol.roleassignments.api.RoleAssignmentResponseDTO;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.ng.accesscontrol.migrations.models.AccessControlMigration;
import io.harness.ng.accesscontrol.migrations.models.RoleAssignmentMetadata;

import java.util.Set;

@OwnedBy(HarnessTeam.PL)
public class AccessControlMigrationMorphiaRegistrar implements MorphiaRegistrar {
  @Override
  public void registerClasses(Set<Class> set) {
    set.add(AccessControlMigration.class);
    set.add(RoleAssignmentMetadata.class);
    set.add(RoleAssignmentResponseDTO.class);
    set.add(RoleAssignmentDTO.class);
  }

  @Override
  public void registerImplementationClasses(MorphiaRegistrarHelperPut h, MorphiaRegistrarHelperPut w) {
    // no class to register
  }
}
