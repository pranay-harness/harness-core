/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.scheduler;

import software.wings.app.MainConfiguration;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ServiceJobScheduler extends JobScheduler {
  @Inject
  public ServiceJobScheduler(Injector injector, MainConfiguration configuration) {
    super(injector, configuration.getServiceSchedulerConfig(), configuration.getMongoConnectionFactory().getUri());
  }
}
