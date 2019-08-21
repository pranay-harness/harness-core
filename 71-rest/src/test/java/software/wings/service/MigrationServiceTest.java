package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import io.harness.category.element.UnitTests;
import migrations.MigrationList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationServiceTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void versionsShouldBeUnique() {
    Set<Integer> versions = new HashSet<>();
    MigrationList.getMigrations().forEach(pair -> {
      assertThat(versions.contains(pair.getKey())).isFalse();
      versions.add(pair.getKey());
    });
  }

  @Test
  @Category(UnitTests.class)
  public void versionsShouldBeSequential() {
    AtomicInteger last = new AtomicInteger(-1);
    MigrationList.getMigrations().forEach(pair -> {
      if (last.get() == -1) {
        last.set(pair.getKey() - 1);
      }
      assertEquals("Schema version " + pair.getKey() + " is not sequential", last.get(), pair.getKey() - 1);
      last.set(pair.getKey());
    });
    assertNotEquals("No items in migration list", last.get(), -1);
  }
}
