package io.harness.resourcegroup.migrations;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.MigrationDetails;
import io.harness.migration.NGMigration;
import io.harness.migration.beans.MigrationType;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(PL)
public class ResourceGroupBackgroundMigrationDetails implements MigrationDetails {
  @Override
  public MigrationType getMigrationTypeName() {
    return MigrationType.MongoBGMigration;
  }

  @Override
  public boolean isBackground() {
    return true;
  }

  @Override
  public List<Pair<Integer, Class<? extends NGMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends NGMigration>>>()
        .add(Pair.of(1, ResourceGroupAllowedScopeLevelsMigration.class))
        .add(Pair.of(2, MultipleManagedResourceGroupDeletionMigration.class))
        .build();
  }
}
