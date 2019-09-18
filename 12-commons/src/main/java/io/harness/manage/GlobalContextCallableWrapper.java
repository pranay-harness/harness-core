package io.harness.manage;

import io.harness.context.GlobalContext;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;
import lombok.Builder;
import lombok.Value;

import java.util.concurrent.Callable;

@Value
@Builder
public class GlobalContextCallableWrapper<T> implements Callable<T> {
  private Callable<T> task;
  private GlobalContext context;

  @Override
  public T call() throws Exception {
    try (GlobalContextGuard guard = new GlobalContextGuard(context)) {
      return task.call();
    }
  }
}
