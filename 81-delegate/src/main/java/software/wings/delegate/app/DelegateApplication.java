package software.wings.delegate.app;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static software.wings.utils.message.MessageConstants.DELEGATE_DASH;
import static software.wings.utils.message.MessageConstants.NEW_DELEGATE;
import static software.wings.utils.message.MessageConstants.WATCHER_DATA;
import static software.wings.utils.message.MessageConstants.WATCHER_HEARTBEAT;
import static software.wings.utils.message.MessageConstants.WATCHER_PROCESS;
import static software.wings.utils.message.MessengerType.DELEGATE;
import static software.wings.utils.message.MessengerType.WATCHER;

import com.google.common.base.Splitter;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import com.ning.http.client.AsyncHttpClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.serializer.YamlUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import software.wings.delegate.service.DelegateService;
import software.wings.managerclient.ManagerClientModule;
import software.wings.utils.message.MessageService;
import software.wings.verification.VerificationServiceClientModule;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public class DelegateApplication {
  private static final Logger logger = LoggerFactory.getLogger(DelegateApplication.class);

  private static String processId;

  public static String getProcessId() {
    return processId;
  }

  public static void main(String... args) throws IOException {
    processId = Splitter.on("@").split(ManagementFactory.getRuntimeMXBean().getName()).iterator().next();

    // Optionally remove existing handlers attached to j.u.l root logger
    SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)

    // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
    // the initialization phase of your application
    SLF4JBridgeHandler.install();

    // Set logging level
    java.util.logging.LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

    initializeLogging();

    File configFile = new File(args[0]);

    String watcherProcess = null;
    if (args.length > 1 && StringUtils.equals(args[1], "watched")) {
      watcherProcess = args[2];
    }
    logger.info("Starting Delegate");
    logger.info("Process: {}", ManagementFactory.getRuntimeMXBean().getName());
    DelegateApplication delegateApplication = new DelegateApplication();
    final DelegateConfiguration configuration =
        new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), DelegateConfiguration.class);
    delegateApplication.run(configuration, watcherProcess);
  }

  @SuppressFBWarnings("DM_EXIT")
  private void run(DelegateConfiguration configuration, String watcherProcess) {
    List<Module> modules = new ArrayList<>();
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(DelegateConfiguration.class).toInstance(configuration);
      }
    });

    modules.add(new ManagerClientModule(
        configuration.getManagerUrl(), configuration.getAccountId(), configuration.getAccountSecret()));
    modules.addAll(new DelegateModule().cumulativeDependencies());
    modules.add(new VerificationServiceClientModule(configuration.getVerificationServiceUrl()));

    Injector injector = Guice.createInjector(modules);
    final MessageService messageService = injector.getInstance(MessageService.class);

    // Add JVM shutdown hook so as to have a clean shutdown
    addShutdownHook(injector, messageService);

    boolean watched = watcherProcess != null;
    if (watched) {
      logger.info("Sending watcher {} new delegate process ID: {}", watcherProcess, processId);
      messageService.writeMessageToChannel(WATCHER, watcherProcess, NEW_DELEGATE, processId);
      Map<String, Object> watcherData = new HashMap<>();
      watcherData.put(WATCHER_HEARTBEAT, System.currentTimeMillis());
      watcherData.put(WATCHER_PROCESS, watcherProcess);
      messageService.putAllData(WATCHER_DATA, watcherData);
    }
    DelegateService delegateService = injector.getInstance(DelegateService.class);
    delegateService.run(watched);

    System.exit(0);
  }

  private void addShutdownHook(Injector injector, MessageService messageService) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      messageService.closeChannel(DELEGATE, processId);
      messageService.closeData(DELEGATE_DASH + processId);
      logger.info("Message service has been closed.");

      // This should run in case of upgrade flow otherwise never called
      injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("heartbeatExecutor"))).shutdownNow();
      injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("localHeartbeatExecutor")))
          .shutdownNow();
      injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("upgradeExecutor"))).shutdownNow();
      injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("inputExecutor"))).shutdownNow();
      injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("taskPollExecutor"))).shutdownNow();
      injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("verificationExecutor"))).shutdownNow();
      injector.getInstance(Key.get(ExecutorService.class, Names.named("verificationDataCollector"))).shutdownNow();
      injector.getInstance(ExecutorService.class).shutdown();
      logger.info("Executor services have been shut down.");

      injector.getInstance(AsyncHttpClient.class).close();
      logger.info("Async HTTP client has been closed.");

      LogManager.shutdown();
      logger.info("Log manager has been shutdown and logs have been flushed.");
    }));
  }
}
