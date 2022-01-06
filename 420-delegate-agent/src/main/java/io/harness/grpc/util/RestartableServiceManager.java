/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.util;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class RestartableServiceManager {
  @Inject private Injector injector;
  private ServiceManager serviceManager;

  public void start() {
    log.info("Initializing grpc server...");
    synchronized (this) {
      ServiceManager sm = injector.getInstance(ServiceManager.class);
      try {
        sm.startAsync().awaitHealthy();
      } catch (Exception e) {
        log.error("failed to start grpc server with error", e);
        return;
      }

      serviceManager = sm;
      log.info("Started grpc server");
    }
  }

  public void stop() {
    log.info("Stopping grpc server...");
    synchronized (this) {
      if (serviceManager == null) {
        return;
      }

      try {
        serviceManager.stopAsync().awaitStopped();
      } catch (Exception e) {
        log.error("failed to stop grpc server with error", e);
        return;
      }

      log.info("Stopped grpc server");
    }
  }

  public boolean isHealthy() {
    if (serviceManager == null) {
      return false;
    }

    return serviceManager.isHealthy();
  }

  // If any of the service is in running state, it returns true.
  public boolean isRunning() {
    if (serviceManager == null) {
      return false;
    }

    ImmutableMultimap<Service.State, Service> serviceByState = serviceManager.servicesByState();
    for (Service service : serviceByState.values()) {
      if (service.isRunning()) {
        return true;
      }
    }
    return false;
  }
}
