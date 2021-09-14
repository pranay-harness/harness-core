/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.platform.resourcegroup;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.platform.PlatformConfiguration.getResourceGroupServiceResourceClasses;

import io.harness.annotations.dev.OwnedBy;
import io.harness.controller.PrimaryVersionChangeScheduler;
import io.harness.health.HealthService;
import io.harness.metrics.jobs.RecordMetricsJob;
import io.harness.metrics.service.api.MetricService;
import io.harness.ng.core.CorrelationFilter;
import io.harness.outbox.OutboxEventPollService;
import io.harness.persistence.HPersistence;
import io.harness.remote.CharsetResponseFilter;
import io.harness.resource.VersionInfoResource;
import io.harness.resourcegroup.ResourceGroupServiceConfig;
import io.harness.resourcegroup.reconciliation.ResourceGroupAsyncReconciliationHandler;
import io.harness.resourcegroup.reconciliation.ResourceGroupSyncConciliationService;

import com.google.inject.Injector;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.model.Resource;

@Slf4j
@OwnedBy(PL)
public class ResourceGroupServiceSetup {
  public static final String RESOURCE_GROUP_SERVICE = "ResourceGroupService";

  public ResourceGroupServiceSetup() {
    // sonar
  }

  public void setup(ResourceGroupServiceConfig appConfig, Environment environment, Injector injector) {
    // Will create collections and Indexes
    injector.getInstance(HPersistence.class);
    registerResources(environment, injector);
    registerCharsetResponseFilter(environment, injector);
    registerCorrelationFilter(environment, injector);
    registerIterators(injector);
    registerScheduledJobs(injector);
    registerManagedBeans(environment, injector);
    registerHealthCheck(environment, injector);
    initializeMonitoring(appConfig, injector);
  }

  private void registerHealthCheck(Environment environment, Injector injector) {
    final HealthService healthService = injector.getInstance(HealthService.class);
    environment.healthChecks().register("ResourceGroup Application", healthService);
    healthService.registerMonitor(injector.getInstance(HPersistence.class));
  }

  private void registerIterators(Injector injector) {
    injector.getInstance(ResourceGroupAsyncReconciliationHandler.class).registerIterators();
  }

  private void initializeMonitoring(ResourceGroupServiceConfig appConfig, Injector injector) {
    if (appConfig.isExportMetricsToStackDriver()) {
      injector.getInstance(MetricService.class).initializeMetrics();
      injector.getInstance(RecordMetricsJob.class).scheduleMetricsTasks();
    }
  }

  private void registerScheduledJobs(Injector injector) {
    injector.getInstance(PrimaryVersionChangeScheduler.class).registerExecutors();
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(ResourceGroupSyncConciliationService.class));
    environment.lifecycle().manage(injector.getInstance(OutboxEventPollService.class));
  }

  private void registerResources(Environment environment, Injector injector) {
    for (Class<?> resource : getResourceGroupServiceResourceClasses()) {
      if (Resource.isAcceptable(resource)) {
        environment.jersey().register(injector.getInstance(resource));
      }
    }
    environment.jersey().register(injector.getInstance(VersionInfoResource.class));
  }

  private void registerCharsetResponseFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CharsetResponseFilter.class));
  }

  private void registerCorrelationFilter(Environment environment, Injector injector) {
    environment.jersey().register(injector.getInstance(CorrelationFilter.class));
  }
}
