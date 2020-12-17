package io.harness.cvng.migrations;

import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.migration.CVNGBackgroundMigrationList;
import io.harness.rule.Owner;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

public class CVNGMigrationServiceTest extends CvNextGenTest {
  private static Answer executeRunnable(ArgumentCaptor<Runnable> runnableCaptor) {
    return invocation -> {
      runnableCaptor.getValue().run();
      return null;
    };
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void versionsShouldBeUnique() {
    ExecutorService executorService = Mockito.mock(ExecutorService.class);
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    when(executorService.submit(runnableCaptor.capture())).then(executeRunnable(runnableCaptor));

    Set<Integer> versions = new HashSet<>();
    CVNGBackgroundMigrationList.getMigrations().forEach(pair -> {
      assertThat(versions.contains(pair.getKey())).isFalse();
      versions.add(pair.getKey());
    });
  }

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void versionsShouldBeSequential() {
    ExecutorService executorService = Mockito.mock(ExecutorService.class);
    ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
    when(executorService.submit(runnableCaptor.capture())).then(executeRunnable(runnableCaptor));

    AtomicInteger last = new AtomicInteger(-1);
    CVNGBackgroundMigrationList.getMigrations().forEach(pair -> {
      if (last.get() == -1) {
        last.set(pair.getKey() - 1);
      }
      assertThat(pair.getKey() - 1).isEqualTo(last.get());
      last.set(pair.getKey());
    });
    assertThat(last.get()).isNotEqualTo(-1);
  }
}
