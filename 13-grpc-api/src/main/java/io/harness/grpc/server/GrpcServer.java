package io.harness.grpc.server;

import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.MoreExecutors;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.services.HealthStatusManager;
import io.harness.logging.LoggingListener;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;

@Slf4j
@ParametersAreNonnullByDefault
public class GrpcServer extends AbstractIdleService {
  private final Server server;
  private final HealthStatusManager healthStatusManager;

  GrpcServer(Connector connector, Set<BindableService> services, Set<ServerInterceptor> interceptors,
      HealthStatusManager healthStatusManager) {
    ServerBuilder<?> builder = ServerBuilder.forPort(connector.getPort());
    if (connector.isSecure()) {
      File certChain = new File(connector.getCertFilePath());
      File privateKey = new File(connector.getKeyFilePath());
      builder = builder.useTransportSecurity(certChain, privateKey);
    }
    interceptors.forEach(builder::intercept);
    services.forEach(builder::addService);
    server = builder.build();
    this.healthStatusManager = healthStatusManager;
    addListener(new LoggingListener(this), MoreExecutors.directExecutor());
  }

  @Override
  protected void startUp() throws Exception {
    logger.info("Starting xxxxxxxx {}", server);
    server.start();
    healthStatusManager.setStatus(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, ServingStatus.SERVING);
    logger.info("Server started successfully");
  }

  @Override
  protected void shutDown() throws Exception {
    logger.info("Stopping xxxxxxxx {}", server);
    healthStatusManager.setStatus(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, ServingStatus.NOT_SERVING);
    server.shutdown();
    server.awaitTermination();
    logger.info("Server stopped");
  }
}
