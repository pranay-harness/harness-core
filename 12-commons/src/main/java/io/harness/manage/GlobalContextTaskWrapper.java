package io.harness.manage;

import io.harness.context.GlobalContext;
import io.harness.manage.GlobalContextManager.GlobalContextGuard;
import lombok.Builder;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Value
@Builder
public class GlobalContextTaskWrapper implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(GlobalContextTaskWrapper.class);
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
