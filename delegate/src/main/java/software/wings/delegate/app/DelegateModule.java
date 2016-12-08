package software.wings.delegate.app;

import com.google.inject.AbstractModule;

import software.wings.common.thread.ThreadPool;
import software.wings.delegate.service.DelegateService;
import software.wings.delegate.service.DelegateServiceImpl;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public class DelegateModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(DelegateService.class).to(DelegateServiceImpl.class);
    bind(ScheduledExecutorService.class).toInstance(new ScheduledThreadPoolExecutor(1));
    bind(ExecutorService.class).toInstance(ThreadPool.create(1, 1, 0, TimeUnit.MILLISECONDS));
  }
}
