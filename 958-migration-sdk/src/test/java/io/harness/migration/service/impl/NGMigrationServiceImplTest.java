package io.harness.migration.service.impl;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.migration.beans.MigrationType;
import io.harness.migration.entities.NGSchema;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.service.impl.MigrationServiceImpl;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NGMigrationServiceImplTest extends CategoryTest {
  @Inject MigrationServiceImpl migrationService;
  @Inject private HPersistence persistence;

  @Test
  @Owner(developers = OwnerRule.MEENAKSHI)
  @Category(UnitTests.class)
  public void testMigrationWhenSchemaIsNull() {
    testUpgradeMigration();
  }

  private void testUpgradeMigration() {
    Map<MigrationType, Integer> mapType = new HashMap<>();
    NGSchema schema = NGSchema.builder().name("dummy").migrationDetails(mapType).build();
  }
}
