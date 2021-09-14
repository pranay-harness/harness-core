/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.grpc.server;

import io.harness.grpc.InterceptorPriority;
import io.harness.logging.LoggingListener;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.services.HealthStatusManager;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.ParametersAreNonnullByDefault;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ParametersAreNonnullByDefault
public class GrpcInProcessServer extends AbstractIdleService {
  private final Server server;
  private final HealthStatusManager healthStatusManager;
  public static final int GRPC_MAXIMUM_MESSAGE_SIZE = 26214400;

  public GrpcInProcessServer(String name, Set<BindableService> services, Set<ServerInterceptor> interceptors,
      HealthStatusManager healthStatusManager) {
    ServerBuilder<?> builder = InProcessServerBuilder.forName(name);
    builder.maxInboundMessageSize(GRPC_MAXIMUM_MESSAGE_SIZE);
    sortedInterceptors(interceptors).forEach(builder::intercept);
    services.forEach(builder::addService);
    server = builder.build();
    this.healthStatusManager = healthStatusManager;
    addListener(new LoggingListener(this), MoreExecutors.directExecutor());
  }

  @Override
  protected void startUp() throws Exception {
    log.info("Starting xxxxxxxx {}", server);
    server.start();
    healthStatusManager.setStatus(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, ServingStatus.SERVING);
    log.info("Server started successfully");
  }

  @Override
  protected void shutDown() throws Exception {
    log.info("Stopping xxxxxxxx {}", server);
    healthStatusManager.setStatus(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, ServingStatus.NOT_SERVING);
    server.shutdown();
    server.awaitTermination();
    log.info("Server stopped");
  }

  private Stream<ServerInterceptor> sortedInterceptors(Set<ServerInterceptor> interceptorSet) {
    return interceptorSet.stream().sorted(
        Comparator
            .comparingInt(interceptor
                -> Optional.ofNullable(interceptor.getClass().getAnnotation(InterceptorPriority.class))
                       .map(InterceptorPriority::value)
                       .orElse(Integer.MAX_VALUE))
            .reversed());
  }
}
