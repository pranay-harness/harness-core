package io.harness.ng;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.migration.MigrationDetails;
import io.harness.migration.MigrationProvider;
import io.harness.migration.NGMigration;
import io.harness.migration.beans.MigrationType;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(DX)
public class NGCoreMigrationProvider implements MigrationProvider {
  @Override
  public String getServiceName() {
    return "ngmanager";
  }

  @Override
  public List<Class<? extends MigrationDetails>> getMigrationDetailsList() {
    return new ArrayList<Class<? extends MigrationDetails>>() {
      { add(NoopMigrationDetailsClass.class); }
    };
  }

  public static class NoopMigrationDetailsClass implements MigrationDetails {
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
          .add(Pair.of(1, NoopNGMigrationClass.class))
          .build();
    }
  }

  public static class NoopNGMigrationClass implements NGMigration {
    @Override
    public void migrate() {
      // do nothing
    }
  }
}