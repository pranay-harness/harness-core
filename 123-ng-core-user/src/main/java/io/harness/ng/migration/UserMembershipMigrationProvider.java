package io.harness.ng.migration;

import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.entities.NGSchema;

import java.util.ArrayList;
import java.util.List;

public class UserMembershipMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "usermembership";
  }

  @Override
  public Class<? extends NGSchema> getSchemaClass() {
    return UserMembershipSchema.class;
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      {
        add(UserMembershipMigrationDetails.class);
        add(UserMembershipStaleScopeMigrationDetails.class);
      }
    };
  }
}