/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ABHINAV;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.migrations.OnPrimaryManagerMigration;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Schema;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.Query;

@OwnedBy(HarnessTeam.CDP)
public class MigrationServiceImplTest extends WingsBaseTest {
  @Inject MigrationServiceImpl migrationService;
  @Inject private HPersistence persistence;

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testMigrationsOnPrimary() {
    testMigrationUpgrade();
    testMigrationDowngrade();
  }

  private void testMigrationUpgrade() {
    Schema schema = Schema.builder()
                        .version(0)
                        .backgroundVersion(0)
                        .seedDataVersion(0)
                        .timescaleDbVersion(0)
                        .timescaleDBDataVersion(0)
                        .onPrimaryManagerVersion(0)
                        .build();
    persistence.save(schema);

    Map<Integer, Class<? extends OnPrimaryManagerMigration>> onPrimaryManagerMigrationMap = new HashMap<>();
    onPrimaryManagerMigrationMap.put(1, TestClass.class);
    migrationService.runMigrationWhenNewManagerIsPrimary(1, onPrimaryManagerMigrationMap);
    Query<Schema> query = persistence.createQuery(Schema.class);
    assertThat(query.get().getOnPrimaryManagerVersion()).isEqualTo(1);
  }

  private void testMigrationDowngrade() {
    Schema schema = Schema.builder()
                        .version(0)
                        .backgroundVersion(0)
                        .seedDataVersion(0)
                        .timescaleDbVersion(0)
                        .timescaleDBDataVersion(0)
                        .onPrimaryManagerVersion(2)
                        .build();
    persistence.save(schema);

    Map<Integer, Class<? extends OnPrimaryManagerMigration>> onPrimaryManagerMigrationMap = new HashMap<>();
    onPrimaryManagerMigrationMap.put(1, TestClass.class);
    migrationService.scheduleOnPrimaryMigrations(schema, 1, onPrimaryManagerMigrationMap);
    Query<Schema> query = persistence.createQuery(Schema.class);
    assertThat(query.get().getOnPrimaryManagerVersion()).isEqualTo(1);
  }

  public static class TestClass implements OnPrimaryManagerMigration {
    @Override
    public void migrate() {
      // do nothing.
    }
  }
}
