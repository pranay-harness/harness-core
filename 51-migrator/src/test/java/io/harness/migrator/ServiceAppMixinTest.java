package io.harness;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.beans.database.MigrationJobInstance;
import io.harness.category.element.UnitTests;
import io.harness.migrator.MigratorTest;
import io.harness.migrator.ServiceAppMixin;
import io.harness.persistence.HPersistence;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServiceAppMixinTest extends MigratorTest {
  @Inject ServiceAppMixin serviceAppMixin;
  @Inject HPersistence persistence;

  @Test
  @Category(UnitTests.class)
  public void testUpdateStoreMigrationJobInstances() {
    serviceAppMixin.updateStoreMigrationJobInstances(HPersistence.DEFAULT_STORE);

    final long count = persistence.createQuery(MigrationJobInstance.class).count();
    assertThat(count).isGreaterThan(0);
  }
}
