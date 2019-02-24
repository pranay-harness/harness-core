package software.wings.watcher.app;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.delegate.message.MessageConstants.NEW_WATCHER;
import static io.harness.delegate.message.MessengerType.WATCHER;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.delegate.message.MessageService;
import io.harness.serializer.YamlUtils;
import io.harness.threading.ExecutorModule;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import software.wings.managerclient.ManagerClientModule;
import software.wings.watcher.service.WatcherService;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created by brett on 10/26/17
 */
public class WatcherApplication {
  private static final Logger logger = LoggerFactory.getLogger(WatcherApplication.class);

  private static String processId;

  public static String getProcessId() {
    return processId;
  }

  public static void main(String... args) throws Exception {
    processId = Splitter.on("@").split(ManagementFactory.getRuntimeMXBean().getName()).iterator().next();
    // Optionally remove existing handlers attached to j.u.l root logger
    SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)

    // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
    // the initialization phase of your application
    SLF4JBridgeHandler.install();

    // Set logging level
    java.util.logging.LogManager.getLogManager().getLogger("").setLevel(Level.INFO);
    File configFile = new File(args[0]);
    boolean upgrade = false;
    String previousWatcherProcess = null;
    if (args.length > 1 && StringUtils.equals(args[1], "upgrade")) {
      upgrade = true;
      previousWatcherProcess = args[2];
    }

    logger.info("Starting Watcher");
    logger.info("Process: {}", ManagementFactory.getRuntimeMXBean().getName());
    WatcherApplication watcherApplication = new WatcherApplication();
    final WatcherConfiguration configuration =
        new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), WatcherConfiguration.class);
    watcherApplication.run(configuration, upgrade, previousWatcherProcess);
  }

  @SuppressFBWarnings("DM_EXIT")
  private void run(WatcherConfiguration configuration, boolean upgrade, String previousWatcherProcess)
      throws Exception {
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
        bind(WatcherConfiguration.class).toInstance(configuration);
      }
    });

    modules.add(new ManagerClientModule(
        configuration.getManagerUrl(), configuration.getAccountId(), configuration.getAccountSecret()));

    modules.addAll(new WatcherModule().cumulativeDependencies());

    Injector injector = Guice.createInjector(modules);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      MessageService messageService = injector.getInstance(MessageService.class);
      messageService.closeChannel(WATCHER, processId);
      logger.info("My watch has ended");
      LogManager.shutdown();
    }));

    if (upgrade) {
      MessageService messageService = injector.getInstance(MessageService.class);
      logger.info("Sending previous watcher process {} new watcher process ID: {}", previousWatcherProcess, processId);
      messageService.writeMessageToChannel(WATCHER, previousWatcherProcess, NEW_WATCHER, processId);
    }

    WatcherService watcherService = injector.getInstance(WatcherService.class);
    watcherService.run(upgrade);

    // This should run in case of upgrade flow otherwise never called
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("inputExecutor"))).shutdownNow();
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("watchExecutor"))).shutdownNow();
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("upgradeExecutor"))).shutdownNow();
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("commandCheckExecutor"))).shutdownNow();
    injector.getInstance(ExecutorService.class).shutdown();
    injector.getInstance(ExecutorService.class).awaitTermination(5, TimeUnit.MINUTES);
    logger.info("Flushing logs");
    LogManager.shutdown();
    System.exit(0);
  }
}
