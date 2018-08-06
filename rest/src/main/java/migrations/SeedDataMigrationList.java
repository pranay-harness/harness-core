package migrations;

import com.google.common.collect.ImmutableList;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class SeedDataMigrationList {
  public static List<Pair<Integer, Class<? extends SeedDataMigration>>> getMigrations() {
    return new ImmutableList.Builder<Pair<Integer, Class<? extends SeedDataMigration>>>()
        .add(Pair.of(1, BaseSeedDataMigration.class))
        .build();
  }
}
