package io.harness.ng;

import com.google.common.collect.ImmutableList;

import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.NGMigration;
import io.harness.migration.beans.MigrationType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class NoOpNGCoreMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "ngmanager";
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      { add(TestMigrationDetailsClass.class); }
    };
  }


  public static class TestMigrationDetailsClass implements MigrationDetails {
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
      return new ImmutableList.Builder<Pair<Integer, Class<? extends NGMigration>>>()
          .add(Pair.of(1, TestNGMigrationClass.class))
          .build();
    }
  }

  public static class TestNGMigrationClass implements NGMigration {
    @Override
    public void migrate() {
      // do nothing
    }
  }
}