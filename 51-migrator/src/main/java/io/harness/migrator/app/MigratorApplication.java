package io.harness.migrator.app;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.serializer.YamlUtils;
import io.harness.threading.ExecutorModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Slf4j
public class MigratorApplication {
  public static void main(String... args) throws Exception {
    // Optionally remove existing handlers attached to j.u.l root logger
    SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)

    // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
    // the initialization phase of your application
    SLF4JBridgeHandler.install();

    // Set logging level
    java.util.logging.LogManager.getLogManager().getLogger("").setLevel(Level.INFO);
    File configFile = new File(args[0]);

    logger.info("Starting migrator");
    logger.info("Process: {}", ManagementFactory.getRuntimeMXBean().getName());
    MigratorApplication migratorApplication = new MigratorApplication();
    final MigratorConfiguration configuration = new YamlUtils().read(
        FileUtils.readFileToString(configFile, StandardCharsets.UTF_8), MigratorConfiguration.class);
    migratorApplication.run(configuration);
  }

  private void run(MigratorConfiguration configuration) {
    int cores = Runtime.getRuntime().availableProcessors();
    int corePoolSize = 2 * cores;
    int maximumPoolSize = Math.max(corePoolSize, 200);

    ExecutorModule.getInstance().setExecutorService(
        new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("watcher-task-%d").build()));

    Set<Module> modules = new HashSet<>();

    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return configuration.getMongoConnectionFactory();
      }
    });
    modules.add(new MongoModule());
    modules.addAll(new MigratorModule().cumulativeDependencies());

    Guice.createInjector(modules);
  }
}
