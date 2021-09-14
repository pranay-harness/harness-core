/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sample.cv;

import static io.harness.logging.LoggingInitializer.initializeLogging;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.maintenance.MaintenanceController;
import io.harness.pms.sample.cv.creator.CvPipelineServiceInfoProvider;
import io.harness.pms.sample.cv.creator.filters.CVFilterCreationResponseMerger;
import io.harness.pms.sdk.PmsSdkConfiguration;
import io.harness.pms.sdk.PmsSdkInitHelper;
import io.harness.pms.sdk.PmsSdkModule;
import io.harness.pms.sdk.core.SdkDeployMode;

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
    List<Module> modules = new ArrayList<>();
    modules.add(new CvServiceModule(config));
    modules.add(PmsSdkModule.getInstance(getPmsSdkConfiguration(config)));
    Injector injector = Guice.createInjector(modules);

    registerJerseyProviders(environment, injector);

    PmsSdkConfiguration sdkConfig = getPmsSdkConfiguration(config);
    try {
      PmsSdkInitHelper.initializeSDKInstance(injector, sdkConfig);
    } catch (Exception e) {
      log.error("Failed To register pipeline sdk", e);
      System.exit(1);
    }

    MaintenanceController.forceMaintenance(false);
  }

  private PmsSdkConfiguration getPmsSdkConfiguration(CvServiceConfiguration config) {
    return PmsSdkConfiguration.builder()
        .deploymentMode(SdkDeployMode.REMOTE)
        .moduleType(ModuleType.CV)
        .grpcServerConfig(config.getPmsSdkGrpcServerConfig())
        .pmsGrpcClientConfig(config.getPmsGrpcClientConfig())
        .pipelineServiceInfoProviderClass(CvPipelineServiceInfoProvider.class)
        .filterCreationResponseMerger(new CVFilterCreationResponseMerger())
        .engineSteps(CvServiceStepRegistrar.getEngineSteps())
        .build();
  }

  private void registerJerseyProviders(Environment environment, Injector injector) {
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(EarlyEofExceptionMapper.class);

    environment.jersey().register(MultiPartFeature.class);
  }
}
