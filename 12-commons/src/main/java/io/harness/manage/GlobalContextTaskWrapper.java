package io.harness.manage;

import io.harness.context.GlobalContext;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;
import lombok.Builder;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Value
@Builder
@Slf4j
public class GlobalContextTaskWrapper implements Runnable {
  private Runnable task;
  private GlobalContext context;

  public GlobalContextTaskWrapper(Runnable task, GlobalContext context) {
    this.task = task;
    this.context = context;
  }

  @Override
  public void run() {
    try (GlobalContextGuard guard = new GlobalContextGuard(context)) {
      task.run();
    } catch (IOException e) {
      logger.error("Something went wrong in task execution.. ALERT", e);
    }
  }
}
