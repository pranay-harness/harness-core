package software.wings.delegate.app;

import static software.wings.utils.message.MessengerType.DELEGATE;
import static software.wings.utils.message.MessengerType.WATCHER;

import com.google.common.base.Splitter;
import com.google.common.io.CharStreams;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import com.ning.http.client.AsyncHttpClient;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import software.wings.delegate.service.DelegateService;
import software.wings.managerclient.ManagerClientModule;
import software.wings.utils.YamlUtils;
import software.wings.utils.message.MessageService;

import java.io.FileReader;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public class DelegateApplication {
  private final static Logger logger = LoggerFactory.getLogger(DelegateApplication.class);

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

    String configFile = args[0];

    if (args.length > 1 && StringUtils.equals(args[1], "watched")) {
      // Watched path
      String watcherProcess = args[2];
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        MessageService messageService = Guice.createInjector(new DelegateModule()).getInstance(MessageService.class);
        messageService.closeChannel(DELEGATE, processId);
        messageService.closeData("delegate-" + processId);
        logger.info("Log manager shutdown hook executing.");
        LogManager.shutdown();
      }));
      logger.info("Starting Delegate");
      logger.info("Process: {}", ManagementFactory.getRuntimeMXBean().getName());
      DelegateApplication delegateApplication = new DelegateApplication();
      delegateApplication.run(
          new YamlUtils().read(CharStreams.toString(new FileReader(configFile)), DelegateConfiguration.class),
          watcherProcess);
    } else {
      // TODO - Legacy path. Remove once watcher is standard
      boolean upgrade = false;
      boolean restart = false;
      if (args.length > 1 && StringUtils.equals(args[1], "upgrade")) {
        upgrade = true;
      }
      if (args.length > 1 && StringUtils.equals(args[1], "restart")) {
        restart = true;
      }
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        logger.info("Log manager shutdown hook executing.");
        LogManager.shutdown();
      }));
      logger.info("Starting Delegate");
      logger.info("Process: {}", ManagementFactory.getRuntimeMXBean().getName());
      DelegateApplication delegateApplication = new DelegateApplication();
      delegateApplication.runLegacy(
          new YamlUtils().read(CharStreams.toString(new FileReader(configFile)), DelegateConfiguration.class), upgrade,
          restart);
    }
  }

  private void runLegacy(DelegateConfiguration configuration, boolean upgrade, boolean restart) throws Exception {
    // TODO - Legacy path. Remove once watcher is standard
    Injector injector = Guice.createInjector(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(DelegateConfiguration.class).toInstance(configuration);
          }
        },
        new ManagerClientModule(
            configuration.getManagerUrl(), configuration.getAccountId(), configuration.getAccountSecret()),
        new DelegateModule());
    DelegateService delegateService = injector.getInstance(DelegateService.class);
    delegateService.run(false, upgrade, restart);

    // This should run in case of upgrade flow otherwise never called
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("heartbeatExecutor"))).shutdownNow();
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("upgradeExecutor"))).shutdownNow();
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("verificationExecutor"))).shutdownNow();
    injector.getInstance(ExecutorService.class).shutdown();
    injector.getInstance(ExecutorService.class).awaitTermination(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
    injector.getInstance(AsyncHttpClient.class).close();
    logger.info("Flushing logs");
    LogManager.shutdown();
    System.exit(0);
  }

  private void run(DelegateConfiguration configuration, String watcherProcess) throws Exception {
    Injector injector = Guice.createInjector(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(DelegateConfiguration.class).toInstance(configuration);
          }
        },
        new ManagerClientModule(
            configuration.getManagerUrl(), configuration.getAccountId(), configuration.getAccountSecret()),
        new DelegateModule());

    MessageService messageService = injector.getInstance(MessageService.class);
    messageService.sendMessage(WATCHER, watcherProcess, "new-delegate", processId);

    DelegateService delegateService = injector.getInstance(DelegateService.class);
    delegateService.run(true, false, false);

    // This should run in case of upgrade flow otherwise never called
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("heartbeatExecutor"))).shutdownNow();
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("localHeartbeatExecutor"))).shutdownNow();
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("upgradeExecutor"))).shutdownNow();
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("inputExecutor"))).shutdownNow();
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("verificationExecutor"))).shutdownNow();
    injector.getInstance(ExecutorService.class).shutdown();
    injector.getInstance(ExecutorService.class).awaitTermination(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
    injector.getInstance(AsyncHttpClient.class).close();
    logger.info("Flushing logs");
    LogManager.shutdown();
    System.exit(0);
  }
}
