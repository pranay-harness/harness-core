package io.harness.manage;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.manage.GlobalContextManager.ensureGlobalContextGuard;
import static io.harness.manage.GlobalContextManager.initGlobalContextGuard;
import static io.harness.manage.GlobalContextManager.upsertGlobalContextRecord;
import static io.harness.rule.OwnerRule.ADWAIT;
import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.context.GlobalContext;
import io.harness.logging.AutoLogContext;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.slf4j.MDC;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class ManagedExecutorServiceTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEnsureGlobalContext() {
    try (GlobalContextGuard contextGuard1 = ensureGlobalContextGuard()) {
      try (GlobalContextGuard contextGuard2 = ensureGlobalContextGuard()) {
        assertThat(GlobalContextManager.isAvailable()).isTrue();
      }
      assertThat(GlobalContextManager.isAvailable()).isTrue();
    }
    assertThat(GlobalContextManager.isAvailable()).isFalse();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testMdcGlobalContext() throws Exception {
    try (GlobalContextGuard globalContextGuard = initGlobalContextGuard(new GlobalContext());
         AutoLogContext ignore = new AutoLogContext(ImmutableMap.of("foo", "bar"), OVERRIDE_ERROR)) {
      ExecutorService executor = Executors.newSingleThreadExecutor();
      ManagedExecutorService managedExecutorService = new ManagedExecutorService(executor);

      Future future = managedExecutorService.submit(() -> { return MDC.get("foo"); });

      assertThat(future.get()).isEqualTo("bar");

      executor.shutdown();
    }
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void testSubmitRunnable() throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    final AtomicBoolean isGlobalContextSetInChildThread = new AtomicBoolean(false);
    final AtomicBoolean isTestContextSetInChildThread = new AtomicBoolean(false);

    ManagedExecutorService managedExecutorService = new ManagedExecutorService(executor);
    try (GlobalContextGuard ignore = initGlobalContextGuard(new GlobalContext())) {
      upsertGlobalContextRecord(() -> "AUDIT_KEY");
      upsertGlobalContextRecord(() -> "TEST_KEY");

      // managedExecutorService.submit(Runnable task) is expected to make sure, new thread executing runnable task,
      // gets GlobalContext from thread creating it.
      Future future = managedExecutorService.submit(() -> {
        GlobalContext globalContext = GlobalContextManager.obtainGlobalContext();
        isGlobalContextSetInChildThread.set(globalContext.get("AUDIT_KEY") != null);
        isTestContextSetInChildThread.set(globalContext.get("TEST_KEY") != null);
      });

      future.get();

      assertThat(isGlobalContextSetInChildThread.get()).isTrue();
      assertThat(isTestContextSetInChildThread.get()).isTrue();
      executor.shutdown();
    }
  }
}
