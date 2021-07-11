package io.harness.migrations;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.MigrationDetails;
import io.harness.migration.NGMigration;
import io.harness.migration.beans.MigrationType;
import io.harness.migrations.timescale.CreateInstanceStatsDayTable;
import io.harness.migrations.timescale.CreateInstanceStatsHourTable;
import io.harness.migrations.timescale.CreateInstanceStatsTable;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(DX)
public class InstanceStatsTimeScaleMigrationDetails implements MigrationDetails {
  @Override
  public MigrationType getMigrationTypeName() {
    return MigrationType.TimeScaleMigration;
  }

  @Override
  public boolean isBackground() {
    return false;
  }

  @Override
  public List<Pair<Integer, Class<? extends NGMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends NGMigration>>>()
        .add(Pair.of(1, CreateInstanceStatsTable.class))
        .add(Pair.of(2, CreateInstanceStatsHourTable.class))
        .add(Pair.of(3, CreateInstanceStatsDayTable.class))
        .build();
  }
}
