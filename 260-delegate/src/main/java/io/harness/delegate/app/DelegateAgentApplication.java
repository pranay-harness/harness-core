package io.harness.delegate.app;

import static io.harness.logging.LoggingInitializer.initializeLogging;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.delegate.app.modules.DelegateAgentModule;
import io.harness.delegate.configuration.DelegateConfiguration;
import io.harness.delegate.resources.DelegateAgentHealthResource;
import io.harness.delegate.service.DelegateAgentService;
import io.harness.event.client.EventPublisher;
import io.harness.grpc.pingpong.PingPongClient;
import io.harness.health.HealthMonitor;
import io.harness.health.HealthService;
import io.harness.persistence.HPersistence;
import io.harness.resource.VersionInfoResource;
import io.harness.serializer.YamlUtils;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;

import ch.qos.logback.classic.LoggerContext;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.ning.http.client.AsyncHttpClient;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

@Slf4j
public class DelegateAgentApplication extends Application<DelegateAgentConfig> {
  // TODO: Two config classes only needed while we have two entrypoints. Get rid of one when removing
  // DelegateApplication.
  private final DelegateConfiguration configuration;

  public DelegateAgentApplication(final String configFileName) throws IOException {
    final File configFile = new File(configFileName);
    try {
      configuration = new YamlUtils().read(FileUtils.readFileToString(configFile, UTF_8), DelegateConfiguration.class);
    } catch (final IOException e) {
      log.error("Unable to read the delegate config file {}", configFileName, e);
      throw e;
    }
  }

  public static void main(final String... args) throws Exception {
    new DelegateAgentApplication(args[1]).run(args);
  }

  private static void setupProxyConfig() {
    final String proxyUser = System.getenv("PROXY_USER");
    if (isNotBlank(proxyUser)) {
      System.setProperty("http.proxyUser", proxyUser);
      System.setProperty("https.proxyUser", proxyUser);
    }
    final String proxyPassword = System.getenv("PROXY_PASSWORD");
    if (isNotBlank(proxyPassword)) {
      System.setProperty("http.proxyPassword", proxyPassword);
      System.setProperty("https.proxyPassword", proxyPassword);
    }
  }

  @Override
  public void run(final DelegateAgentConfig delegateAgentConfig, final Environment environment) throws Exception {
    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(10, 40, 1, TimeUnit.SECONDS,
        new ThreadFactoryBuilder().setNameFormat("sync-task-%d").setPriority(Thread.NORM_PRIORITY).build()));

    final Injector injector = Guice.createInjector(new DelegateAgentModule(configuration));

    addShutdownHook(injector);
    registerResources(environment, injector);
    registerHealthChecks(environment, injector);
    injector.getInstance(PingPongClient.class).startAsync();

    log.info("Starting Delegate");
    log.info("Process: {}", ManagementFactory.getRuntimeMXBean().getName());

    injector.getInstance(DelegateAgentService.class).run(false);
  }

  @Override
  public void initialize(final Bootstrap<DelegateAgentConfig> bootstrap) {
    super.initialize(bootstrap);

    setupProxyConfig();
    // Optionally remove existing handlers attached to j.u.l root logger
    SLF4JBridgeHandler.removeHandlersForRootLogger(); // (since SLF4J 1.6.5)
    // add SLF4JBridgeHandler to j.u.l's root logger, should be done once during
    // the initialization phase of your application
    SLF4JBridgeHandler.install();
    // Set logging level
    java.util.logging.LogManager.getLogManager().getLogger("").setLevel(Level.INFO);

    initializeLogging();
  }

  private void addShutdownHook(final Injector injector) {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      injector.getInstance(ExecutorService.class).shutdown();
      injector.getInstance(EventPublisher.class).shutdown();
      log.info("Executor services have been shut down.");

      injector.getInstance(PingPongClient.class).stopAsync();
      log.info("PingPong client have been shut down.");

      injector.getInstance(AsyncHttpClient.class).close();
      log.info("Async HTTP client has been closed.");

      final ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
      if (loggerFactory instanceof LoggerContext) {
        final LoggerContext context = (LoggerContext) loggerFactory;
        context.stop();
      }
      log.info("Log manager has been shutdown and logs have been flushed.");
    }));
  }

  private void registerHealthChecks(final Environment environment, final Injector injector) {
    final HealthService healthService = injector.getInstance(HealthService.class);

    healthService.registerMonitor(injector.getInstance(HealthMonitor.class));
    environment.healthChecks().register("DelegateAgentApp", healthService);
  }

  private void registerResources(final Environment environment, final Injector injector) {
    // environment.jersey().register(injector.getInstance(VersionInfoResource.class));
    environment.jersey().register(injector.getInstance(DelegateAgentHealthResource.class));
  }
}
