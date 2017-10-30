package software.wings.watcher.app;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import org.apache.commons.codec.binary.StringUtils;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import software.wings.watcher.service.WatcherService;

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
    boolean upgrade = false;
    if (args.length > 0 && StringUtils.equals(args[0], "upgrade")) {
      upgrade = true;
    }

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("Log manager shutdown hook executing.");
      LogManager.shutdown();
    }));
    logger.info("Starting Watcher");
    logger.info("Process: {}", ManagementFactory.getRuntimeMXBean().getName());
    WatcherApplication watcherApplication = new WatcherApplication();
    watcherApplication.run(upgrade);
  }

  private void run(boolean upgrade) throws Exception {
    Injector injector = Guice.createInjector(new WatcherModule());
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
