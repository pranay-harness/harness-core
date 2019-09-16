package io.harness.manage;

import io.harness.context.GlobalContext;
import io.harness.context.GlobalContextData;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;

@Slf4j
public class GlobalContextManager {
  private static final ThreadLocal<GlobalContext> contextThreadLocal = new ThreadLocal<>();

  public static class GlobalContextGuard implements Closeable {
    private boolean unset;
    protected GlobalContextGuard(GlobalContext globalContext) {
      if (unset = globalContext != null) {
        set(globalContext);
      }
    }

    @Override
    public void close() {
      if (unset) {
        unset();
      }
    }
  }

  public static GlobalContextGuard ensureGlobalContextGuard() {
    GlobalContext globalContext = contextThreadLocal.get();
    if (globalContext != null) {
      return new GlobalContextGuard(null);
    }
    globalContext = new GlobalContext();
    return new GlobalContextGuard(globalContext);
  }

  public static GlobalContextGuard initGlobalContextGuard(GlobalContext globalContext) {
    if (globalContext == null) {
      globalContext = new GlobalContext();
    }
    return new GlobalContextGuard(globalContext);
  }

  public static void set(GlobalContext globalContext) {
    contextThreadLocal.set(globalContext);
  }

  public static void unset() {
    contextThreadLocal.remove();
  }

  public static GlobalContextData get(String key) {
    GlobalContext globalContext = contextThreadLocal.get();
    if (globalContext == null) {
      logger.warn("Global Context was null. Seems thread was not initialized. ALERT....");
      // logger.error("Global Context was null. Seems thread was not initialized. ALERT....", new Exception());
      return null;
    }

    return globalContext.get(key);
  }

  public static GlobalContextTaskWrapper generateExecutorTask(Runnable task) {
    return GlobalContextTaskWrapper.builder().task(task).context(contextThreadLocal.get()).build();
  }

  public static void upsertGlobalContextRecord(GlobalContextData data) {
    GlobalContext globalContext = contextThreadLocal.get();
    if (globalContext == null) {
      logger.warn("Global Context was null. Seems thread was not initialized. ALERT....");
      return;
    }

    globalContext.upsertGlobalContextRecord(data);
  }

  public static boolean isAvailable() {
    return contextThreadLocal.get() != null;
  }

  public static GlobalContext getGlobalContext() {
    return contextThreadLocal.get();
  }
}