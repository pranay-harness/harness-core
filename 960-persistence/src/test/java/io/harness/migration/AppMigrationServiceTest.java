/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migration;

import static io.harness.migration.MigrationJobInstance.Status.BASELINE;
import static io.harness.migration.MigrationJobInstance.Status.PENDING;
import static io.harness.rule.OwnerRule.GEORGE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PersistenceTestBase;
import io.harness.category.element.UnitTests;
import io.harness.migration.MigrationJobInstance.MigrationJobInstanceKeys;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AppMigrationServiceTest extends PersistenceTestBase {
  @Inject AppMigrationService serviceAppMixin;
  @Inject HPersistence persistence;

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testInitBaseline() {
    serviceAppMixin.updateStoreMigrationJobInstances(HPersistence.DEFAULT_STORE);

    final long count =
        persistence.createQuery(MigrationJobInstance.class).filter(MigrationJobInstanceKeys.status, BASELINE).count();
    assertThat(count).isEqualTo(MigrationList.jobs.size());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testUpdateStoreMigrationJobInstances() {
    final MigrationJobInstance dummyInstance = MigrationJobInstance.builder().build();
    persistence.save(dummyInstance);
    persistence.delete(dummyInstance);

    serviceAppMixin.updateStoreMigrationJobInstances(HPersistence.DEFAULT_STORE);

    final long count =
        persistence.createQuery(MigrationJobInstance.class).filter(MigrationJobInstanceKeys.status, PENDING).count();
    assertThat(count).isGreaterThan(0);
  }
}
