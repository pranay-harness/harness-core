package io.harness.commandlibrary.server.app;

import static java.util.Objects.requireNonNull;

import io.harness.commandlibrary.server.service.impl.CommandServiceImpl;
import io.harness.commandlibrary.server.service.impl.CommandStoreServiceImpl;
import io.harness.commandlibrary.server.service.impl.CommandVersionServiceImpl;
import io.harness.commandlibrary.server.service.impl.CustomDeploymentTypeArchiveHandler;
import io.harness.commandlibrary.server.service.impl.ServiceCommandArchiveHandler;
import io.harness.commandlibrary.server.service.intfc.CommandArchiveHandler;
import io.harness.commandlibrary.server.service.intfc.CommandService;
import io.harness.commandlibrary.server.service.intfc.CommandStoreService;
import io.harness.commandlibrary.server.service.intfc.CommandVersionService;
import io.harness.exception.UnexpectedException;
import io.harness.persistence.HPersistence;
import io.harness.queue.QueueController;
import io.harness.threading.ThreadPool;
import io.harness.version.VersionInfoManager;

import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.FeatureFlagServiceImpl;
import software.wings.service.impl.MongoDataStoreServiceImpl;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.intfc.DataStoreService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.SecretManager;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.IOUtils;

public class CommandLibraryServerModule extends AbstractModule {
  private CommandLibraryServerConfig configuration;

  public CommandLibraryServerModule(CommandLibraryServerConfig configuration) {
    this.configuration = configuration;
  }

  @Override
  protected void configure() {
    bind(CommandLibraryServerConfig.class).toInstance(configuration);
    bind(HPersistence.class).to(WingsMongoPersistence.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class);
    bind(SecretManager.class).to(NoOpSecretManagerImpl.class);
    bind(Clock.class).toInstance(Clock.systemUTC());

    bind(TimeLimiter.class).toInstance(new SimpleTimeLimiter());
    bind(FeatureFlagService.class).to(FeatureFlagServiceImpl.class);
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

    bind(QueueController.class).toInstance(new QueueController() {
      @Override
      public boolean isNotPrimary() {
        return false;
      }

      @Override
      public boolean isPrimary() {
        return true;
      }
    });

    try {
      VersionInfoManager versionInfoManager = new VersionInfoManager(IOUtils.toString(
          requireNonNull(getClass().getClassLoader().getResourceAsStream("main/resources-filtered/versionInfo.yaml")),
          StandardCharsets.UTF_8));
      bind(VersionInfoManager.class).toInstance(versionInfoManager);
    } catch (IOException e) {
      throw new UnexpectedException("Could not load versionInfo.yaml", e);
    }
  }

  private void bindCommandArchiveHandlers() {
    final Multibinder<CommandArchiveHandler> commandArchiveHandlerBinder =
        Multibinder.newSetBinder(binder(), CommandArchiveHandler.class);
    commandArchiveHandlerBinder.addBinding().to(ServiceCommandArchiveHandler.class);
    commandArchiveHandlerBinder.addBinding().to(CustomDeploymentTypeArchiveHandler.class);
  }
}
