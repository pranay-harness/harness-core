package io.harness;

import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.packages.HarnessPackages.IO_HARNESS;

import io.harness.gitsync.AbstractGitSyncSdkModule;
import io.harness.gitsync.GitSyncSdkConfiguration;
import io.harness.gitsync.interceptor.GitSyncThreadDecorator;
import io.harness.maintenance.MaintenanceController;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.ws.rs.Path;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.model.Resource;
import org.reflections.Reflections;

@Slf4j
public class GitSyncTestApplication extends Application<GitSyncTestConfiguration> {
  private static final String APPLICATION_NAME = "Git Sync Sample Application";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));

    new GitSyncTestApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<GitSyncTestConfiguration> bootstrap) {
    initializeLogging();
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
  }

  @Override
  public void run(GitSyncTestConfiguration config, Environment environment) {
    log.info("Starting Git Sync Application ...");
    MaintenanceController.forceMaintenance(true);
    List<Module> modules = new ArrayList<>();
    modules.add(new GitSyncTestModule(config));
    modules.add(new AbstractGitSyncSdkModule() {
      @Override
      public GitSyncSdkConfiguration getGitSyncSdkConfiguration() {
        final Supplier<List<EntityType>> sortOrder = () -> Collections.singletonList(EntityType.CONNECTORS);
        return GitSyncSdkConfiguration.builder()
            .gitSyncSortOrder(sortOrder)
            .grpcClientConfig(config.getGrpcClientConfig())
            .build();
      }
    });
    Injector injector = Guice.createInjector(modules);

    registerJerseyProviders(environment, injector);
    registerResources(environment, injector);
    MaintenanceController.forceMaintenance(false);
  }

  private void registerResources(Environment environment, Injector injector) {
    Reflections reflections = new Reflections(IO_HARNESS);
    final Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(Path.class);
    for (Class<?> resource : typesAnnotatedWith) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
  }

  private void registerJerseyProviders(Environment environment, Injector injector) {
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(EarlyEofExceptionMapper.class);
    environment.jersey().register(GitSyncThreadDecorator.class);
    environment.jersey().register(MultiPartFeature.class);
  }
}