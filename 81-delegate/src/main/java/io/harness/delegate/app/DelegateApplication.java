package io.harness.delegate.app;

import static com.google.common.base.Charsets.UTF_8;
import static io.harness.configuration.DeployMode.DEPLOY_MODE;
import static io.harness.configuration.DeployMode.isOnPrem;
import static io.harness.delegate.message.MessageConstants.DELEGATE_DASH;
import static io.harness.delegate.message.MessageConstants.NEW_DELEGATE;
import static io.harness.delegate.message.MessageConstants.WATCHER_DATA;
import static io.harness.delegate.message.MessageConstants.WATCHER_HEARTBEAT;
import static io.harness.delegate.message.MessageConstants.WATCHER_PROCESS;
import static io.harness.delegate.message.MessengerType.DELEGATE;
import static io.harness.delegate.message.MessengerType.WATCHER;
import static io.harness.delegate.service.DelegateServiceImpl.getDelegateId;
import static io.harness.grpc.utils.DelegateGrpcConfigExtractor.extractAuthority;
import static io.harness.grpc.utils.DelegateGrpcConfigExtractor.extractTarget;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.common.base.Splitter;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

import com.ning.http.client.AsyncHttpClient;
import io.harness.delegate.configuration.DelegateConfiguration;
import io.harness.delegate.message.MessageService;
import io.harness.delegate.service.DelegateService;
import io.harness.event.client.EventPublisher;
import io.harness.event.client.impl.EventPublisherConstants;
import io.harness.event.client.impl.appender.AppenderModule;
import io.harness.event.client.impl.appender.AppenderModule.Config;
import io.harness.grpc.ManagerGrpcClientModule;
import io.harness.grpc.pingpong.PingPongClient;
import io.harness.grpc.pingpong.PingPongModule;
import io.harness.managerclient.ManagerClientModule;
import io.harness.perpetualtask.PerpetualTaskWorkerModule;
import io.harness.serializer.KryoModule;
import io.harness.serializer.YamlUtils;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.slf4j.bridge.SLF4JBridgeHandler;
import software.wings.delegatetasks.k8s.apiclient.KubernetesApiClientFactoryModule;
import software.wings.delegatetasks.k8s.client.KubernetesClientFactoryModule;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
@Slf4j
public class DelegateApplication {
  private static String processId;
  private static DelegateConfiguration configuration;

  public static String getProcessId() {
    return processId;
  }

  public static DelegateConfiguration getConfiguration() {
    return configuration;
  }

  public static void main(String... args) throws IOException {
    try {
      processId = Splitter.on("@").split(ManagementFactory.getRuntimeMXBean().getName()).iterator().next();

      String proxyUser = System.getenv("PROXY_USER");
      if (isNotBlank(proxyUser)) {
        System.setProperty("http.proxyUser", proxyUser);
        System.setProperty("https.proxyUser", proxyUser);
      }
      String proxyPassword = System.getenv("PROXY_PASSWORD");
      if (isNotBlank(proxyPassword)) {
        System.setProperty("http.proxyPassword", proxyPassword);
        System.setProperty("https.proxyPassword", proxyPassword);
      }

      File configFile = new File(args[0]);
      configuration = new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), DelegateConfiguration.class);

      String watcherProcess = null;
      if (args.length > 1 && StringUtils.equals(args[1], "watched")) {
        watcherProcess = args[2];
      }

      // Optionally remove existing handlers attached to j.u.l root logger
      SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)

      // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
      // the initialization phase of your application
      SLF4JBridgeHandler.install();

      // Set logging level
      java.util.logging.LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

      initializeLogging();
      logger.info("Starting Delegate");
      logger.info("Process: {}", ManagementFactory.getRuntimeMXBean().getName());
      DelegateApplication delegateApplication = new DelegateApplication();
      delegateApplication.run(configuration, watcherProcess);
    } catch (RuntimeException | IOException exception) {
      logger.error("Delegate process initialization failed", exception);
      throw exception;
    }
  }

  private void run(DelegateConfiguration configuration, String watcherProcess) {
    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(10, 40, 1, TimeUnit.SECONDS,
        new ThreadFactoryBuilder().setNameFormat("sync-task-%d").setPriority(Thread.NORM_PRIORITY).build()));

    List<Module> modules = new ArrayList<>();
    modules.add(new KryoModule());
    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(DelegateConfiguration.class).toInstance(configuration);
      }
    });
    modules.add(new ManagerClientModule(configuration.getManagerUrl(), configuration.getVerificationServiceUrl(),
        configuration.getAccountId(), configuration.getAccountSecret()));
    String managerHostAndPort = System.getenv("MANAGER_HOST_AND_PORT");
    modules.add(new ManagerGrpcClientModule(
        ManagerGrpcClientModule.Config.builder()
            .target(Optional.ofNullable(configuration.getManagerTarget())
                        .orElseGet(() -> extractTarget(managerHostAndPort)))
            .authority(Optional.ofNullable(configuration.getManagerAuthority())
                           .orElseGet(() -> extractAuthority(managerHostAndPort, "manager")))
            .accountId(configuration.getAccountId())
            .accountSecret(configuration.getAccountSecret())
            .build()));
    if (!isOnPrem(System.getenv().get(DEPLOY_MODE))) {
      modules.add(new PingPongModule());
      modules.add(new PerpetualTaskWorkerModule());
    }
    modules.add(new KubernetesClientFactoryModule());
    modules.add(new KubernetesApiClientFactoryModule());
    modules.add(new AppenderModule(Config.builder()
                                       .queueFilePath(Optional.ofNullable(configuration.getQueueFilePath())
                                                          .orElse(EventPublisherConstants.DEFAULT_QUEUE_FILE_PATH))
                                       .build(),
        () -> getDelegateId().orElse("UNREGISTERED")));
    modules.addAll(new DelegateModule().cumulativeDependencies());

    Injector injector = Guice.createInjector(modules);
    MessageService messageService = injector.getInstance(MessageService.class);

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
    if (!isOnPrem(System.getenv().get(DEPLOY_MODE))) {
      injector.getInstance(PingPongClient.class).startAsync();
    }
    Runtime.getRuntime().addShutdownHook(new Thread(() -> injector.getInstance(PingPongClient.class).stopAsync()));
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
      injector.getInstance(Key.get(ExecutorService.class, Names.named("alternativeExecutor"))).shutdownNow();

      injector.getInstance(ExecutorService.class).shutdown();
      injector.getInstance(EventPublisher.class).shutdown();
      logger.info("Executor services have been shut down.");

      injector.getInstance(AsyncHttpClient.class).close();
      logger.info("Async HTTP client has been closed.");

      LogManager.shutdown();
      logger.info("Log manager has been shutdown and logs have been flushed.");
    }));
  }
}
