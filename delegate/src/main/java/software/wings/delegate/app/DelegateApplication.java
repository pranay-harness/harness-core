package software.wings.delegate.app;

import com.google.common.io.CharStreams;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import com.ning.http.client.AsyncHttpClient;
import org.apache.commons.codec.binary.StringUtils;
import org.slf4j.bridge.SLF4JBridgeHandler;
import software.wings.delegate.service.DelegateService;
import software.wings.managerclient.ManagerClientModule;
import software.wings.utils.YamlUtils;

import java.io.FileReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public class DelegateApplication {
  static {
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
    DelegateApplication delegateApplication = new DelegateApplication();
    delegateApplication.run(
        new YamlUtils().read(CharStreams.toString(new FileReader(configFile)), DelegateConfiguration.class), upgrade);
  }

  public void run(DelegateConfiguration configuration, boolean upgrade) throws Exception {
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
    delegateService.run(upgrade);

    // This should run in case of upgrade flow otherwise never called
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("heartbeatExecutor"))).shutdownNow();
    injector.getInstance(Key.get(ScheduledExecutorService.class, Names.named("upgradeExecutor"))).shutdownNow();
    injector.getInstance(ExecutorService.class).shutdown();
    injector.getInstance(ExecutorService.class).awaitTermination(Integer.MAX_VALUE, TimeUnit.MILLISECONDS);
    injector.getInstance(AsyncHttpClient.class).close();
    System.exit(0);
  }
}
