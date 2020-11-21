package io.harness.manage;

import io.harness.context.GlobalContext;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GlobalContextTaskWrapper implements Runnable {
  private Runnable task;
  private GlobalContext context;

  @Override
  public void run() {
    try (GlobalContextGuard guard = new GlobalContextGuard(context)) {
      task.run();
    }
  }
}
