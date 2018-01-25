package software.wings.service;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.service.impl.MigrationServiceImpl;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationServiceTest extends WingsBaseTest {
  @Test
  public void versionsShouldBeUnique() {
    Set<Integer> versions = new HashSet<>();
    MigrationServiceImpl.migrationList.forEach(pair -> {
      assertFalse(
          "Duplicate schema version " + pair.getKey() + " in migrations list", versions.contains(pair.getKey()));
      versions.add(pair.getKey());
    });
  }

  @Test
  public void versionsShouldBeSequential() {
    AtomicInteger last = new AtomicInteger(-1);
    MigrationServiceImpl.migrationList.forEach(pair -> {
      if (last.get() == -1) {
        last.set(pair.getKey() - 1);
      }
      assertTrue("Schema version " + pair.getKey() + " is not sequential", last.get() == pair.getKey() - 1);
      last.set(pair.getKey());
    });
    assertFalse("No items in migration list", last.get() == -1);
  }
}
