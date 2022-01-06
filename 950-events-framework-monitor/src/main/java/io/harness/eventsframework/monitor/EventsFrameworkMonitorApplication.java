/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.eventsframework.monitor;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.eventsframework.monitor.publisher.EventsFrameworkMonitoringRunner;
import io.harness.logging.LoggingInitializer;
import io.harness.maintenance.MaintenanceController;
import io.harness.metrics.MetricRegistryModule;
import io.harness.metrics.jobs.RecordMetricsJob;
import io.harness.metrics.service.api.MetricService;
import io.harness.queue.QueueListenerController;
import io.harness.remote.NGObjectMapperHelper;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.protobuf.InvalidProtocolBufferException;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.io.UnsupportedEncodingException;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import service.RedisStreamsMetricsAggregator;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class EventsFrameworkMonitorApplication extends Application<EventsFrameworkMonitorConfiguration> {
  private static final String APPLICATION_NAME = "Events Framework Monitoring application";

  public static void main(String[] args) throws Exception {
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      log.info("Shutdown hook, entering maintenance...");
      MaintenanceController.forceMaintenance(true);
    }));
    new EventsFrameworkMonitorApplication().run(args);
  }

  private final MetricRegistry metricRegistry = new MetricRegistry();

  @Override
  public String getName() {
    return APPLICATION_NAME;
  }

  @Override
  public void initialize(Bootstrap<EventsFrameworkMonitorConfiguration> bootstrap) {
    LoggingInitializer.initializeLogging();
    // Enable variable substitution with environment variables
    bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
        bootstrap.getConfigurationSourceProvider(), new EnvironmentVariableSubstitutor(false)));
    configureObjectMapper(bootstrap.getObjectMapper());
  }
  public static void configureObjectMapper(final ObjectMapper mapper) {
    NGObjectMapperHelper.configureNGObjectMapper(mapper);
  }

  @Override
  public void run(EventsFrameworkMonitorConfiguration appConfig, Environment environment)
      throws InvalidProtocolBufferException, UnsupportedEncodingException, InterruptedException {
    log.info("Starting Events framework monitor Application ...");
    MaintenanceController.forceMaintenance(true);
    Injector injector =
        Guice.createInjector(new EventsFrameworkMonitorModule(appConfig), new MetricRegistryModule(metricRegistry));

    registerJerseyFeatures(environment);
    registerManagedBeans(environment, injector);

    if (appConfig.isStackDriverMetricsPushEnabled()) {
      injector.getInstance(MetricService.class).initializeMetrics();

      injector.getInstance(RecordMetricsJob.class).scheduleMetricsTasks();
    } else {
      log.info("Stack driver push is not enabled, logging stats via a Runnable");
      new Thread(new EventsFrameworkMonitoringRunner(injector.getInstance(RedisStreamsMetricsAggregator.class)))
          .start();
    }

    MaintenanceController.forceMaintenance(false);
  }

  private void registerJerseyFeatures(Environment environment) {
    environment.jersey().register(MultiPartFeature.class);
  }

  private void registerManagedBeans(Environment environment, Injector injector) {
    environment.lifecycle().manage(injector.getInstance(QueueListenerController.class));
  }
}
