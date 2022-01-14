package io.harness.ng.migration;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.beans.MigrationType;
import io.harness.migration.entities.NGSchema;
import io.harness.ng.DbAliases;

import java.util.Map;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.mongodb.core.mapping.Document;

@StoreIn(DbAliases.NG_MANAGER)
@Document("schema_ngmanager")
@Persistent
@OwnedBy(DX)
public class NGCoreSchema extends NGSchema {
  public NGCoreSchema(
      String id, Long createdAt, Long lastUpdatedAt, String name, Map<MigrationType, Integer> migrationDetails) {
    super(id, createdAt, lastUpdatedAt, name, migrationDetails);
  }
}
