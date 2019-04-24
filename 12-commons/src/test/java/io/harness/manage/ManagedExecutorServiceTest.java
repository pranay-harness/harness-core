package io.harness.manage;

import static org.junit.Assert.assertTrue;

import io.harness.category.element.UnitTests;
import io.harness.context.GlobalContext;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class ManagedExecutorServiceTest {
  @Test
  @Category(UnitTests.class)
  public void testSubmit_Runnable() throws Exception {
    ExecutorService executor = Executors.newSingleThreadExecutor();
    final AtomicBoolean isGlobalContextSetInChildThread = new AtomicBoolean(false);
    final AtomicBoolean isTestContextSetInChildThread = new AtomicBoolean(false);

    ManagedExecutorService managedExecutorService = new ManagedExecutorService(executor);
    GlobalContextManager.initGlobalContextGuard(new GlobalContext());
    GlobalContextManager.upsertGlobalContextRecord(() -> "AUDIT_KEY");
    GlobalContextManager.upsertGlobalContextRecord(() -> "TEST_KEY");

    // managedExecutorService.submit(Runnable task) is expected to make sure, new thread executing runnable task,
    // gets GlobalContext from thread creating it.
    Future future = managedExecutorService.submit(() -> {
      GlobalContext globalContext = GlobalContextManager.getGlobalContext();
      isGlobalContextSetInChildThread.set(globalContext.get("AUDIT_KEY") != null);
      isTestContextSetInChildThread.set(globalContext.get("TEST_KEY") != null);
    });

    future.get();

    assertTrue(isGlobalContextSetInChildThread.get());
    assertTrue(isTestContextSetInChildThread.get());
    executor.shutdown();
  }
}
