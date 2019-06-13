package io.harness.migrator.app;

import static com.google.common.base.Charsets.UTF_8;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import io.harness.mongo.MigratorMorphiaClasses;
import io.harness.mongo.MongoModule;
import io.harness.mongo.PersistenceMorphiaClasses;
import io.harness.serializer.YamlUtils;
import io.harness.threading.ExecutorModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

@Slf4j
public class MigratorApplication {
  public static final Set<Class> morphiaClasses = ImmutableSet.<Class>builder()
                                                      .addAll(MigratorMorphiaClasses.classes)
                                                      .addAll(PersistenceMorphiaClasses.classes)
                                                      .build();

  public static void main(String... args) throws Exception {
    // Optionally remove existing handlers attached to j.u.l root logger
    SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)

    // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
    // the initialization phase of your application
    SLF4JBridgeHandler.install();

    // Set logging level
    java.util.logging.LogManager.getLogManager().getLogger("").setLevel(Level.INFO);
    File configFile = new File(args[0]);
    boolean upgrade = false;
    if (args.length > 1 && StringUtils.equals(args[1], "upgrade")) {
      upgrade = true;
    }

    logger.info("Starting migrator");
    logger.info("Process: {}", ManagementFactory.getRuntimeMXBean().getName());
    MigratorApplication migratorApplication = new MigratorApplication();
    final MigratorConfiguration configuration =
        new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), MigratorConfiguration.class);
    migratorApplication.run(configuration);
  }

  private void run(MigratorConfiguration configuration) throws Exception {
    int cores = Runtime.getRuntime().availableProcessors();
    int corePoolSize = 2 * cores;
    int maximumPoolSize = Math.max(corePoolSize, 200);

    ExecutorModule.getInstance().setExecutorService(
        new ThreadPoolExecutor(corePoolSize, maximumPoolSize, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("watcher-task-%d").build()));

    Set<Module> modules = new HashSet<>();

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(MigratorConfiguration.class).toInstance(configuration);
      }
    });
    modules.add(new MongoModule(configuration.getMongoConnectionFactory(), morphiaClasses));
    modules.addAll(new MigratorModule().cumulativeDependencies());

    Injector injector = Guice.createInjector(modules);
  }
}
