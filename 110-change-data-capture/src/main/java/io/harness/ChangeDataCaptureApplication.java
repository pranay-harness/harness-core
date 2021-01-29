package io.harness;

import io.harness.changestreamsframework.ChangeDataCaptureSyncService;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ChangeDataCaptureApplication extends Application<ChangeDataCaptureServiceConfig> {
  public static void main(String[] args) throws Exception {
    log.info("Starting Change Data Capture Application...");

    new ChangeDataCaptureApplication().run(args);
  }

  @Override
  public void run(ChangeDataCaptureServiceConfig changeDataCaptureServiceConfig, Environment environment)
      throws Exception {
    log.info("Starting Change Data Capture App with Config... {}", changeDataCaptureServiceConfig);

    Injector injector = Guice.createInjector();
    registerManagedBeans(environment, injector);
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(ChangeDataCaptureSyncService.class));
  }
}
