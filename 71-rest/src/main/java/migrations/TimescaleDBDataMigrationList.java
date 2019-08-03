package migrations;

import com.google.common.collect.ImmutableList;

import migrations.timescaledb.data.MigrateWorkflowsToTimeScaleDB;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class TimescaleDBDataMigrationList {
  public static List<Pair<Integer, Class<? extends TimeScaleDBDataMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends TimeScaleDBDataMigration>>>()
        .add(Pair.of(1, MigrateWorkflowsToTimeScaleDB.class))
        .build();
  }
}
