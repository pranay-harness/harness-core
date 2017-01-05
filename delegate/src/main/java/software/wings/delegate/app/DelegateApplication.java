package software.wings.delegate.app;

import com.google.common.io.CharStreams;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import org.apache.commons.codec.binary.StringUtils;
import software.wings.delegate.service.DelegateService;
import software.wings.managerclient.ManagerClientModule;
import software.wings.utils.YamlUtils;

import java.io.FileReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public class DelegateApplication {
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
    injector.getInstance(ExecutorService.class).shutdownNow();
    System.exit(0);
  }
}
