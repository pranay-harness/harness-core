package io.harness.licensing.migration;

import io.harness.licensing.migration.tasks.ModuleToAccountMigration;
import io.harness.migration.MigrationDetails;
import io.harness.migration.NGMigration;
import io.harness.migration.beans.MigrationType;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class LicenseManagerMongoMigrationDetails implements MigrationDetails {
  @Override
  public MigrationType getMigrationTypeName() {
    return MigrationType.MongoMigration;
  }

  @Override
  public boolean isBackground() {
    return false;
  }

  @Override
  public List<Pair<Integer, Class<? extends NGMigration>>> getMigrations() {
    return ImmutableList.<Pair<Integer, Class<? extends NGMigration>>>builder()
        .add(Pair.of(1, ModuleToAccountMigration.class))
        .build();
  }
}
