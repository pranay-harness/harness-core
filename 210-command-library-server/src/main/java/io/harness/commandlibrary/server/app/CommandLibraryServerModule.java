/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.commandlibrary.server.app;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.lock.DistributedLockImplementation.MONGO;

import io.harness.annotations.dev.OwnedBy;
import io.harness.app.PrimaryVersionManagerModule;
import io.harness.commandlibrary.server.service.impl.CommandServiceImpl;
import io.harness.commandlibrary.server.service.impl.CommandStoreServiceImpl;
import io.harness.commandlibrary.server.service.impl.CommandVersionServiceImpl;
import io.harness.commandlibrary.server.service.impl.CustomDeploymentTypeArchiveHandler;
import io.harness.commandlibrary.server.service.impl.ServiceCommandArchiveHandler;
import io.harness.commandlibrary.server.service.intfc.CommandArchiveHandler;
import io.harness.commandlibrary.server.service.intfc.CommandService;
import io.harness.commandlibrary.server.service.intfc.CommandStoreService;
import io.harness.commandlibrary.server.service.intfc.CommandVersionService;
import io.harness.concurrent.HTimeLimiter;
import io.harness.ff.FeatureFlagModule;
import io.harness.lock.DistributedLockImplementation;
import io.harness.persistence.HPersistence;
import io.harness.redis.RedisConfig;
import io.harness.threading.ThreadPool;

import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.MongoDataStoreServiceImpl;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.security.EncryptedSettingAttributes;
import software.wings.service.intfc.security.SecretManager;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@OwnedBy(PL)
public class CommandLibraryServerModule extends AbstractModule {
  private CommandLibraryServerConfig configuration;

  public CommandLibraryServerModule(CommandLibraryServerConfig configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    install(FeatureFlagModule.getInstance());
    install(PrimaryVersionManagerModule.getInstance());

    bind(CommandLibraryServerConfig.class).toInstance(configuration);
    bind(HPersistence.class).to(WingsMongoPersistence.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class);
    bind(SecretManager.class).to(NoOpSecretManagerImpl.class);
    bind(EncryptedSettingAttributes.class).to(NoOpSecretManagerImpl.class);
    bind(Clock.class).toInstance(Clock.systemUTC());

    bind(TimeLimiter.class).toInstance(HTimeLimiter.create());
    bind(CommandStoreService.class).to(CommandStoreServiceImpl.class);
    bind(CommandService.class).to(CommandServiceImpl.class);
    bind(CommandVersionService.class).to(CommandVersionServiceImpl.class);
    bindCommandArchiveHandlers();

    bind(ExecutorService.class)
        .toInstance(ThreadPool.create(1, 20, 5, TimeUnit.SECONDS,
            new ThreadFactoryBuilder()
                .setNameFormat("default-command-library-service-executor-%d")
                .setPriority(Thread.MIN_PRIORITY)
                .build()));

    bind(ScheduledExecutorService.class)
        .annotatedWith(Names.named("command-library-service-executor"))
        .toInstance(new ScheduledThreadPoolExecutor(1,
            new ThreadFactoryBuilder()
                .setNameFormat("command-library-service-Thread-%d")
                .setPriority(Thread.NORM_PRIORITY)
                .build()));

    bind(DataStoreService.class).to(MongoDataStoreServiceImpl.class);
  }

  @Provides
  @Singleton
  DistributedLockImplementation distributedLockImplementation() {
    return MONGO;
  }

  @Provides
  @Named("lock")
  @Singleton
  RedisConfig redisLockConfig() {
    return RedisConfig.builder().build();
  }

  private void bindCommandArchiveHandlers() {
    final Multibinder<CommandArchiveHandler> commandArchiveHandlerBinder =
        Multibinder.newSetBinder(binder(), CommandArchiveHandler.class);
    commandArchiveHandlerBinder.addBinding().to(ServiceCommandArchiveHandler.class);
    commandArchiveHandlerBinder.addBinding().to(CustomDeploymentTypeArchiveHandler.class);
  }
}
