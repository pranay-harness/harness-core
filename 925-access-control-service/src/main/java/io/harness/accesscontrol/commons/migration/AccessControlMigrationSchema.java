package io.harness.accesscontrol.commons.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.DbAliases.ACCESS_CONTROL;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.beans.MigrationType;
import io.harness.migration.entities.NGSchema;

import java.util.Map;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@StoreIn(ACCESS_CONTROL)
@Document("schema_accesscontrol")
@TypeAlias("schema_accesscontrol")
@OwnedBy(PL)
public class AccessControlMigrationSchema extends NGSchema {
  public AccessControlMigrationSchema(
      String id, Long createdAt, Long lastUpdatedAt, String name, Map<MigrationType, Integer> migrationDetails) {
    super(id, createdAt, lastUpdatedAt, name, migrationDetails);
  }
}