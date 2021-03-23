package io.harness.accesscontrol.migrations.models;

public enum MigrationStatus {
  FAILED_AT_RESOURCE_GROUP_UPSERTION_AND_SYNC,
  FAILED_AT_ROLE_ASSIGNMENT_CREATION,
  FAILED_AT_USER_PROJECT_MAP_UPSERTION,
  SUCCESSFUL;
}
