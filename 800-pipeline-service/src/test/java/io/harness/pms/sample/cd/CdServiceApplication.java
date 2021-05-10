package io.harness.pms.sample.cd;

import static io.harness.logging.LoggingInitializer.initializeLogging;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.maintenance.MaintenanceController;
import io.harness.pms.sample.cd.creator.CdPipelineServiceInfoProvider;
import io.harness.pms.sample.cd.creator.filters.CDFilterCreationResponseMerger;
import io.harness.pms.sdk.PmsSdkConfiguration;
import io.harness.pms.sdk.PmsSdkConfiguration.DeployMode;
import io.harness.pms.sdk.PmsSdkInitHelper;
import io.harness.pms.sdk.PmsSdkModule;
import io.harness.pms.sdk.core.execution.listeners.NodeExecutionEventListener;
import io.harness.pms.sdk.core.interrupt.InterruptEventListener;
import io.harness.pms.sdk.execution.SdkOrchestrationEventListener;
import io.harness.queue.QueueListenerController;

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
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

@Slf4j
@OwnedBy(HarnessTeam.PIPELINE)
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
    PmsSdkConfiguration sdkConfig = getPmsSdkConfiguration(config);
    List<Module> modules = new ArrayList<>();
    modules.add(new CdServiceModule(config));
    modules.add(PmsSdkModule.getInstance(sdkConfig));
    Injector injector = Guice.createInjector(modules);

    registerQueueListeners(injector);
    registerJerseyProviders(environment, injector);

    try {
      PmsSdkInitHelper.initializeSDKInstance(injector, sdkConfig);
    } catch (Exception e) {
      log.error("Failed To register pipeline sdk", e);
      System.exit(1);
    }

    MaintenanceController.forceMaintenance(false);
  }

  private PmsSdkConfiguration getPmsSdkConfiguration(CdServiceConfiguration config) {
    return PmsSdkConfiguration.builder()
        .deploymentMode(DeployMode.REMOTE)
        .serviceName("cd")
        .mongoConfig(config.getMongoConfig())
        .grpcServerConfig(config.getPmsSdkGrpcServerConfig())
        .pmsGrpcClientConfig(config.getPmsGrpcClientConfig())
        .pipelineServiceInfoProviderClass(CdPipelineServiceInfoProvider.class)
        .filterCreationResponseMerger(new CDFilterCreationResponseMerger())
        .engineSteps(CdServiceStepRegistrar.getEngineSteps())
        .executionSummaryModuleInfoProviderClass(CDExecutionSummaryModuleInfoProvider.class)
        .build();
  }

  private void registerQueueListeners(Injector injector) {
    log.info("Initializing queue listeners...");
    QueueListenerController queueListenerController = injector.getInstance(QueueListenerController.class);
    queueListenerController.register(injector.getInstance(NodeExecutionEventListener.class), 1);
    queueListenerController.register(injector.getInstance(SdkOrchestrationEventListener.class), 1);
    queueListenerController.register(injector.getInstance(InterruptEventListener.class), 1);
  }

  private void registerJerseyProviders(Environment environment, Injector injector) {
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(EarlyEofExceptionMapper.class);

    environment.jersey().register(MultiPartFeature.class);
  }
}
