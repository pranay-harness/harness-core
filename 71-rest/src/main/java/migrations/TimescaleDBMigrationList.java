package migrations;

import com.google.common.collect.ImmutableList;

import migrations.timescaledb.InitSchemaMigration;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class TimescaleDBMigrationList {
  public static List<Pair<Integer, Class<? extends TimeScaleDBMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends TimeScaleDBMigration>>>()
        .add(Pair.of(1, InitSchemaMigration.class))
        .build();
  }
}
