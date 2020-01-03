package io.harness.migration;

import static io.harness.migration.MigrationJobInstance.Status.BASELINE;
import static io.harness.migration.MigrationJobInstance.Status.PENDING;
import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.PersistenceTest;
import io.harness.category.element.UnitTests;
import io.harness.migration.MigrationJobInstance.MigrationJobInstanceKeys;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AppMigrationServiceTest extends PersistenceTest {
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
