package software.wings.delegate.app;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;

import software.wings.common.thread.ThreadPool;
import software.wings.delegate.service.DelegateFileManagerImpl;
import software.wings.delegate.service.DelegateService;
import software.wings.delegate.service.DelegateServiceImpl;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.helpers.ext.jenkins.JenkinsImpl;

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
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("heartbeatExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1, r -> {
          Thread thread = new Thread(r);
          thread.setName("Heartbeat-Thread");
          thread.setPriority(Thread.MAX_PRIORITY);
          return thread;
        }));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("upgradeExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1, r -> {
          Thread thread = new Thread(r);
          thread.setName("UpgradeCheck-Thread");
          thread.setPriority(Thread.MAX_PRIORITY);
          return thread;
        }));
    bind(ExecutorService.class).toInstance(ThreadPool.create(1, 1, 0, TimeUnit.MILLISECONDS));
    install(new FactoryModuleBuilder().implement(Jenkins.class, JenkinsImpl.class).build(JenkinsFactory.class));
    bind(DelegateFileManager.class).to(DelegateFileManagerImpl.class);
  }
}
