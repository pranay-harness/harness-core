package io.harness.watcher.app;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.name.Names;

import io.harness.delegate.message.MessageService;
import io.harness.delegate.message.MessageServiceImpl;
import io.harness.delegate.message.MessengerType;
import io.harness.govern.DependencyModule;
import io.harness.time.TimeModule;
import io.harness.watcher.service.WatcherService;
import io.harness.watcher.service.WatcherServiceImpl;

import java.time.Clock;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Created by brett on 10/26/17
 */
public class WatcherModule extends DependencyModule {
  @Override
  protected void configure() {
    bind(WatcherService.class).to(WatcherServiceImpl.class);
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("inputExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("InputCheck-Thread").setPriority(Thread.NORM_PRIORITY).build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("heartbeatExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(
            1, new ThreadFactoryBuilder().setNameFormat("Heartbeat-Thread").setPriority(Thread.MAX_PRIORITY).build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("watchExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(
            1, new ThreadFactoryBuilder().setNameFormat("Watch-Thread").setPriority(Thread.MAX_PRIORITY).build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("upgradeExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(
            1, new ThreadFactoryBuilder().setNameFormat("Upgrade-Thread").setPriority(Thread.MAX_PRIORITY).build()));
    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("commandCheckExecutor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder().setNameFormat("CommandCheck-Thread").setPriority(Thread.NORM_PRIORITY).build()));

    bind(MessageService.class)
        .toInstance(
            new MessageServiceImpl(Clock.systemUTC(), MessengerType.WATCHER, WatcherApplication.getProcessId()));
    bind(Clock.class).toInstance(Clock.systemUTC());
  }

  @Override
  public Set<DependencyModule> dependencies() {
    return ImmutableSet.<DependencyModule>of(TimeModule.getInstance());
  }
}
