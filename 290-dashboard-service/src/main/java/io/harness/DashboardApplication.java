package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.maintenance.MaintenanceController;
import io.harness.threading.ExecutorModule;
import io.harness.threading.ThreadPool;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.dropwizard.Application;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.model.Resource;

@OwnedBy(PL)
@Slf4j
public class DashboardApplication extends Application<DashboardServiceConfig> {
  public static void main(String[] args) throws Exception {
    log.info("Starting Dashboards Application...");
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));
    new DashboardApplication().run(args);
  }

  @Override
  public void run(DashboardServiceConfig config, Environment environment) throws Exception {
    log.info("Entering startup maintenance mode");
    MaintenanceController.forceMaintenance(true);

    ExecutorModule.getInstance().setExecutorService(ThreadPool.create(
        1, 10, 500L, TimeUnit.MILLISECONDS, new ThreadFactoryBuilder().setNameFormat("main-app-pool-%d").build()));

    List<Module> modules = new ArrayList<>();
    modules.add(new DashboardServiceModule(config));
    Injector injector = Guice.createInjector(modules);
    registerResources(environment, injector);
    // todo @deepak Add the auth filter
    // todo @deepak Add the correlation filter
    // todo @deepak Add the register for health check
    MaintenanceController.forceMaintenance(false);
  }

  private void registerResources(Environment environment, Injector injector) {
    for (Class<?> resource : DashboardServiceConfig.getResourceClasses()) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }
}
