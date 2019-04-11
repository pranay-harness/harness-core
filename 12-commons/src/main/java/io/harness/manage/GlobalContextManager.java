package io.harness.manage;

import io.harness.context.GlobalContext;
import io.harness.context.GlobalContextData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

public class GlobalContextManager {
  private static final Logger logger = LoggerFactory.getLogger(GlobalContextManager.class);
  public static final ThreadLocal<GlobalContext> contextThreadLocal = new ThreadLocal();

  public static class GlobalContextGuard implements Closeable {
    GlobalContextGuard(GlobalContext globalContext) {
      logger.info("Global Context Update for Thread, Add initiated: " + Thread.currentThread().getId());
      set(globalContext);
    }

    @Override
    public void close() throws IOException {
      logger.info("Global Context Update for Thread, Remove initiated: " + Thread.currentThread().getId());
      unset();
    }
  }
  private static void set(GlobalContext globalContext) {
    contextThreadLocal.set(globalContext);
  }

  private static void unset() {
    contextThreadLocal.remove();
  }

  public static GlobalContextData get(String key) {
    GlobalContext globalContext = contextThreadLocal.get();
    if (globalContext == null) {
      logger.error("Global Context was null. Seems thread was not initialized. ALERT....", new Exception());
      return null;
    }

    return globalContext.get(key);
  }

  public static GlobalContextTaskWrapper generateExecutoTask(Runnable task) {
    return GlobalContextTaskWrapper.builder().task(task).context(contextThreadLocal.get()).build();
  }

  public static void upsertGlobalContextRecord(GlobalContextData data) {
    GlobalContext globalContext = contextThreadLocal.get();
    if (globalContext == null) {
      logger.error("Global Context was null. Seems thread was not initialized. ALERT....", new Exception());
      return;
    }

    globalContext.upsertGlobalContextRecord(data);
  }
}