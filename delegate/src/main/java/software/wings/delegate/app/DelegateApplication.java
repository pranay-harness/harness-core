package software.wings.delegate.app;

import com.google.common.io.CharStreams;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

import software.wings.delegate.service.DelegateService;
import software.wings.managerclient.ManagerClientModule;
import software.wings.utils.YamlUtils;

import java.io.FileReader;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public class DelegateApplication {
  public static void main(String... args) throws Exception {
    String configFile = args[0];
    DelegateApplication delegateApplication = new DelegateApplication();
    delegateApplication.run(
        new YamlUtils().read(CharStreams.toString(new FileReader(configFile)), DelegateConfiguration.class));
  }

  public void run(DelegateConfiguration configuration) throws Exception {
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
    delegateService.run();
  }
}
