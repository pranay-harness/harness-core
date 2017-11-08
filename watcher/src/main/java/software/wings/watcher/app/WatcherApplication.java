package software.wings.watcher.app;

import com.google.common.io.CharStreams;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import software.wings.utils.YamlUtils;
import software.wings.watcher.service.WatcherService;

import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created by brett on 10/26/17
 */
public class WatcherApplication {
  private final static Logger logger = LoggerFactory.getLogger(WatcherApplication.class);

  static {
    String processId = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    System.setProperty("process_id", processId);

    // Optionally remove existing handlers attached to j.u.l root logger
    SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)

    // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
    // the initialization phase of your application
    SLF4JBridgeHandler.install();

    // Set logging level
    java.util.logging.LogManager.getLogManager().getLogger("").setLevel(Level.INFO);
  }

  public static void main(String... args) throws Exception {
    String configFile = args[0];
    boolean upgrade = false;
    if (args.length > 1 && StringUtils.equals(args[1], "upgrade")) {
      upgrade = true;
    }

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("My watch has ended.");
      LogManager.shutdown();
    }));
    logger.info("Starting Watcher");
    logger.info("Process: {}", ManagementFactory.getRuntimeMXBean().getName());
    WatcherApplication watcherApplication = new WatcherApplication();
    watcherApplication.run(
        new YamlUtils().read(CharStreams.toString(new FileReader(configFile)), WatcherConfiguration.class), upgrade);
  }

  private void run(WatcherConfiguration configuration, boolean upgrade) throws Exception {
    Injector injector = Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(WatcherConfiguration.class).toInstance(configuration);
      }
    }, new WatcherModule());
    WatcherService watcherService = injector.getInstance(WatcherService.class);
    watcherService.run(upgrade);

    // This should run in case of upgrade flow otherwise never called
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("upgradeExecutor"))).shutdownNow();
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("watchExecutor"))).shutdownNow();
    injector.getInstance(ExecutorService.class).shutdown();
    injector.getInstance(ExecutorService.class).awaitTermination(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
    logger.info("Flushing logs");
    LogManager.shutdown();
    System.exit(0);
  }
}
