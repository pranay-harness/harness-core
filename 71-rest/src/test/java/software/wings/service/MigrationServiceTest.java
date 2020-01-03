package software.wings.service;

import static io.harness.rule.OwnerRule.BRETT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import migrations.MigrationList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class MigrationServiceTest extends WingsBaseTest {
  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void versionsShouldBeUnique() {
    Set<Integer> versions = new HashSet<>();
    MigrationList.getMigrations().forEach(pair -> {
      assertThat(versions.contains(pair.getKey())).isFalse();
      versions.add(pair.getKey());
    });
  }

  @Test
  @Owner(developers = BRETT)
  @Category(UnitTests.class)
  public void versionsShouldBeSequential() {
    AtomicInteger last = new AtomicInteger(-1);
    MigrationList.getMigrations().forEach(pair -> {
      if (last.get() == -1) {
        last.set(pair.getKey() - 1);
      }
      assertThat(pair.getKey() - 1).isEqualTo(last.get());
      last.set(pair.getKey());
    });
    assertThat(last.get()).isNotEqualTo(-1);
  }
}
