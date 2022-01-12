/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.platform.audit;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.platform.PlatformConfiguration.AUDIT_SERVICE_RESOURCES;

import io.harness.annotations.dev.OwnedBy;
import io.harness.audit.retention.AuditAccountSyncService;
import io.harness.audit.retention.AuditRetentionIteratorHandler;
import io.harness.controller.PrimaryVersionChangeScheduler;
import io.harness.health.HealthService;
import io.harness.ng.core.CorrelationFilter;
import io.harness.persistence.HPersistence;
import io.harness.remote.CharsetResponseFilter;
import io.harness.resource.VersionInfoResource;

import com.google.inject.Injector;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.server.model.Resource;

@Slf4j
@OwnedBy(PL)
public class AuditServiceSetup {
  public static final String AUDIT_SERVICE = "AuditService";

  public AuditServiceSetup() {
    // sonar
  }

  public void setup(AuditServiceConfiguration appConfig, Environment environment, Injector injector) {
    // Will create collections and Indexes
    injector.getInstance(HPersistence.class);
    registerResources(environment, injector);
    registerCharsetResponseFilter(environment, injector);
    registerCorrelationFilter(environment, injector);
    registerHealthCheck(environment, injector);
    registerManagedBeans(environment, injector);
    registerIterators(injector);
    registerScheduledJobs(injector);
  }

  private void registerHealthCheck(Environment environment, Injector injector) {
    final HealthService healthService = injector.getInstance(HealthService.class);
    environment.healthChecks().register("Audit Application", healthService);
    healthService.registerMonitor(injector.getInstance(HPersistence.class));
  }

  private void registerResources(Environment environment, Injector injector) {
    for (Class<?> resource : AUDIT_SERVICE_RESOURCES) {
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

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(AuditAccountSyncService.class));
  }

  private void registerIterators(Injector injector) {
    injector.getInstance(AuditRetentionIteratorHandler.class).registerIterators();
  }

  private void registerScheduledJobs(Injector injector) {
    injector.getInstance(PrimaryVersionChangeScheduler.class).registerExecutors();
  }
}
