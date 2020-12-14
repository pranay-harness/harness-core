package io.harness.pms.sample.cd;

import static io.harness.logging.LoggingInitializer.initializeLogging;

import io.harness.maintenance.MaintenanceController;
import io.harness.persistence.HPersistence;
import io.harness.pms.sample.cd.creator.CdPipelineServiceInfoProvider;
import io.harness.pms.sample.cd.creator.filters.CDFilterCreationResponseMerger;
import io.harness.pms.sdk.PmsSdkConfiguration;
import io.harness.pms.sdk.PmsSdkModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.jersey.errors.EarlyEofExceptionMapper;
import io.dropwizard.jersey.jackson.JsonProcessingExceptionMapper;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

@Slf4j
public class CdServiceApplication extends Application<CdServiceConfiguration> {
  private static final String APPLICATION_NAME = "CD Server Application";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));

    new CdServiceApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<CdServiceConfiguration> bootstrap) {
    initializeLogging();
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
  }

  @Override
  public void run(CdServiceConfiguration config, Environment environment) {
    log.info("Starting Pipeline Service Application ...");
    MaintenanceController.forceMaintenance(true);
    Injector injector = Guice.createInjector(new CdServiceModule(config));

    injector.getInstance(HPersistence.class);
    registerJerseyProviders(environment, injector);

    PmsSdkConfiguration sdkConfig =
        PmsSdkConfiguration.builder()
            .serviceName("cd")
            .mongoConfig(config.getMongoConfig())
            .grpcServerConfig(config.getPmsSdkGrpcServerConfig())
            .pmsGrpcClientConfig(config.getPmsGrpcClientConfig())
            .pipelineServiceInfoProvider(injector.getInstance(CdPipelineServiceInfoProvider.class))
            .filterCreationResponseMerger(new CDFilterCreationResponseMerger())
            .build();
    try {
      PmsSdkModule.initializeDefaultInstance(sdkConfig);
    } catch (Exception e) {
      log.error("Failed To register pipeline sdk");
      System.exit(1);
    }

    MaintenanceController.forceMaintenance(false);
  }

  private void registerJerseyProviders(Environment environment, Injector injector) {
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(EarlyEofExceptionMapper.class);

    environment.jersey().register(MultiPartFeature.class);
  }
}
