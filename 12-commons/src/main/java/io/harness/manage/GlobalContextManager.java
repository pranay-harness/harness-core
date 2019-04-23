package io.harness.manage;

import io.harness.context.GlobalContext;
import io.harness.context.GlobalContextData;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;

@Slf4j
public class GlobalContextManager {
  private static final ThreadLocal<GlobalContext> contextThreadLocal = new ThreadLocal<>();
  public static class GlobalContextGuard implements Closeable {
    GlobalContextGuard(GlobalContext globalContext) {
      set(globalContext);
    }

    @Override
    public void close() throws IOException {
      unset();
    }
  }

  public static GlobalContextGuard globalContextGuard(GlobalContextData globalContextData) {
    GlobalContext globalContext = new GlobalContext();
    globalContext.setGlobalContextRecord(globalContextData);
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

  public static GlobalContext getGlobalContext() {
    return contextThreadLocal.get();
  }
}