package io.harness.ng.migration;

import io.harness.migration.MigrationDetails;
import io.harness.migration.NGMigration;
import io.harness.migration.beans.MigrationType;
import io.harness.ng.core.user.service.impl.UserMembershipMigrationService;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

public class UserMembershipMigrationDetails implements MigrationDetails {
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
        .add(Pair.of(1, UserMembershipMigrationService.class))
        .build();
  }
}
