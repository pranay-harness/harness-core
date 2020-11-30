package io.harness.pms.sample.cv;

import static io.harness.logging.LoggingInitializer.initializeLogging;

import io.harness.maintenance.MaintenanceController;
import io.harness.persistence.HPersistence;
import io.harness.pms.sample.cv.creator.CvPipelineServiceInfoProvider;
import io.harness.pms.sample.cv.creator.filters.CVFilterCreationResponseMerger;
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
public class CvServiceApplication extends Application<CvServiceConfiguration> {
  private static final String APPLICATION_NAME = "CV Server Application";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));

    new CvServiceApplication().run(args);
  }

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<CvServiceConfiguration> bootstrap) {
    initializeLogging();
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
  }

  @Override
  public void run(CvServiceConfiguration config, Environment environment) {
    log.info("Starting Pipeline Service Application ...");
    MaintenanceController.forceMaintenance(true);
    Injector injector = Guice.createInjector(new CvServiceModule(config));

    injector.getInstance(HPersistence.class);
    registerJerseyProviders(environment, injector);

    PmsSdkConfiguration sdkConfig = PmsSdkConfiguration.builder()
                                        .grpcServerConfig(config.getPmsSdkGrpcServerConfig())
                                        .pmsGrpcClientConfig(config.getPmsGrpcClientConfig())
                                        .pipelineServiceInfoProvider(new CvPipelineServiceInfoProvider())
                                        .filterCreationResponseMerger(new CVFilterCreationResponseMerger())
                                        .build();
    PmsSdkModule.initializeDefaultInstance(sdkConfig);

    MaintenanceController.forceMaintenance(false);
  }

  private void registerJerseyProviders(Environment environment, Injector injector) {
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(EarlyEofExceptionMapper.class);

    environment.jersey().register(MultiPartFeature.class);
  }
}
