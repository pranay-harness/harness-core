package io.harness.threading;

import io.harness.govern.DependencyModule;
import io.harness.manage.ManagedExecutorService;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;
import java.util.concurrent.ExecutorService;

public class ExecutorModule extends DependencyModule {
  private static volatile ExecutorModule instance;

  public static ExecutorModule getInstance() {
    if (instance == null) {
      instance = new ExecutorModule();
    }
    return instance;
  }

  @Getter @Setter private ExecutorService executorService;

  @Override
  protected void configure() {
    // that's noop check to trigger PMD friendly NullPointerException
    executorService.isShutdown();

    bind(ExecutorService.class).toInstance(new ManagedExecutorService(executorService));
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return null;
  }
}
