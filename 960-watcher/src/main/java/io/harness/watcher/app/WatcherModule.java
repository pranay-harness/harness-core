/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.watcher.app;

import io.harness.delegate.message.MessageService;
import io.harness.delegate.message.MessageServiceImpl;
import io.harness.delegate.message.MessengerType;
import io.harness.time.TimeModule;
import io.harness.watcher.service.WatcherService;
import io.harness.watcher.service.WatcherServiceImpl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.time.Clock;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class WatcherModule extends AbstractModule {
  private static volatile WatcherModule instance;

  public static WatcherModule getInstance() {
    if (instance == null) {
      instance = new WatcherModule();
    }
    return instance;
  }

  @Provides
  @Singleton
  @Named("inputExecutor")
  public ScheduledExecutorService inputExecutor() {
    ScheduledExecutorService inputExecutor = new ScheduledThreadPoolExecutor(
        1, new ThreadFactoryBuilder().setNameFormat("input-%d").setPriority(Thread.NORM_PRIORITY).build());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> { inputExecutor.shutdownNow(); }));
    return inputExecutor;
  }

  @Provides
  @Singleton
  @Named("heartbeatExecutor")
  public ScheduledExecutorService heartbeatExecutor() {
    ScheduledExecutorService heartbeatExecutor = new ScheduledThreadPoolExecutor(
        1, new ThreadFactoryBuilder().setNameFormat("heartbeat-%d").setPriority(Thread.MAX_PRIORITY).build());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> { heartbeatExecutor.shutdownNow(); }));
    return heartbeatExecutor;
  }

  @Provides
  @Singleton
  @Named("watchExecutor")
  public ScheduledExecutorService watchExecutor() {
    ScheduledExecutorService watchExecutor = new ScheduledThreadPoolExecutor(
        1, new ThreadFactoryBuilder().setNameFormat("watcher-%d").setPriority(Thread.MAX_PRIORITY).build());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> { watchExecutor.shutdownNow(); }));
    return watchExecutor;
  }

  @Provides
  @Singleton
  @Named("upgradeExecutor")
  public ScheduledExecutorService upgradeExecutor() {
    ScheduledExecutorService upgradeExecutor = new ScheduledThreadPoolExecutor(
        1, new ThreadFactoryBuilder().setNameFormat("upgrade-%d").setPriority(Thread.MAX_PRIORITY).build());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> { upgradeExecutor.shutdownNow(); }));
    return upgradeExecutor;
  }

  @Provides
  @Singleton
  @Named("commandCheckExecutor")
  public ScheduledExecutorService commandCheckExecutor() {
    ScheduledExecutorService commandCheckExecutor = new ScheduledThreadPoolExecutor(
        1, new ThreadFactoryBuilder().setNameFormat("commandCheck-%d").setPriority(Thread.MAX_PRIORITY).build());
    Runtime.getRuntime().addShutdownHook(new Thread(() -> { commandCheckExecutor.shutdownNow(); }));
    return commandCheckExecutor;
  }

  @Override
  protected void configure() {
    install(TimeModule.getInstance());

    bind(WatcherService.class).to(WatcherServiceImpl.class);
    bind(MessageService.class)
        .toInstance(
            new MessageServiceImpl("", Clock.systemUTC(), MessengerType.WATCHER, WatcherApplication.getProcessId()));
    bind(Clock.class).toInstance(Clock.systemUTC());
  }
}
