package io.harness.accesscontrol.migrations.models;

import io.harness.accesscontrol.roleassignments.RoleAssignment;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserMigration {
  String principalType;
  String principalIdentifier;
  MigrationStatus migrationStatus;
  List<RoleAssignment> createdRoleAssignments;
  List<RoleAssignment> failedRoleAssignments;
}
