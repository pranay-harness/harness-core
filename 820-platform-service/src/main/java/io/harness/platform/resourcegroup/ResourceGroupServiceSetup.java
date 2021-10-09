package io.harness.platform.resourcegroup;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.platform.PlatformConfiguration.getResourceGroupServiceResourceClasses;

import io.harness.Microservice;
import io.harness.annotations.dev.OwnedBy;
import io.harness.controller.PrimaryVersionChangeScheduler;
import io.harness.health.HealthService;
import io.harness.metrics.jobs.RecordMetricsJob;
import io.harness.metrics.service.api.MetricService;
import io.harness.migration.MigrationProvider;
import io.harness.migration.NGMigrationSdkInitHelper;
import io.harness.migration.beans.NGMigrationConfiguration;
import io.harness.ng.core.CorrelationFilter;
import io.harness.outbox.OutboxEventPollService;
import io.harness.persistence.HPersistence;
import io.harness.remote.CharsetResponseFilter;
import io.harness.resource.VersionInfoResource;
import io.harness.resourcegroup.ResourceGroupServiceConfig;
import io.harness.resourcegroup.ResourceGroupsManagementJob;
import io.harness.resourcegroup.migrations.ResourceGroupMigrationProvider;
import io.harness.resourcegroup.reconciliation.ResourceGroupAsyncReconciliationHandler;
import io.harness.resourcegroup.reconciliation.ResourceGroupSyncConciliationService;

import com.google.inject.Injector;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
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
    registerMigrations(injector);
    registerHealthCheck(environment, injector);
    initializeMonitoring(appConfig, injector);
    ResourceGroupsManagementJob resourceGroupsManagementJob = injector.getInstance(ResourceGroupsManagementJob.class);
    resourceGroupsManagementJob.run();
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

  private void registerMigrations(Injector injector) {
    NGMigrationConfiguration config = getMigrationSdkConfiguration();
    NGMigrationSdkInitHelper.initialize(injector, config);
  }

  private NGMigrationConfiguration getMigrationSdkConfiguration() {
    return NGMigrationConfiguration.builder()
        .microservice(Microservice.RESOURCEGROUP)
        .migrationProviderList(new ArrayList<Class<? extends MigrationProvider>>() {
          { add(ResourceGroupMigrationProvider.class); }
        })
        .build();
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
