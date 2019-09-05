package io.harness.grpc.server;

import com.google.common.util.concurrent.AbstractIdleService;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.health.v1.HealthCheckResponse.ServingStatus;
import io.grpc.services.HealthStatusManager;

import java.io.File;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class GrpcServer extends AbstractIdleService {
  private final Server server;

  private HealthStatusManager healthStatusManager;

  GrpcServer(Connector config, Set<BindableService> services, Set<ServerInterceptor> interceptors,
      HealthStatusManager healthStatusManager) {
    ServerBuilder<?> builder = ServerBuilder.forPort(config.getPort());
    if (config.isSecure()) {
      File certChain = new File(config.getCertFilePath());
      File privateKey = new File(config.getKeyFilePath());
      builder = builder.useTransportSecurity(certChain, privateKey);
    }
    for (ServerInterceptor interceptor : interceptors) {
      builder.intercept(interceptor);
    }
    for (BindableService service : services) {
      builder.addService(service);
    }
    server = builder.build();
    this.healthStatusManager = healthStatusManager;
  }

  @Override
  protected void startUp() throws Exception {
    server.start();
    healthStatusManager.setStatus(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, ServingStatus.SERVING);
  }

  @Override
  protected void shutDown() throws Exception {
    healthStatusManager.setStatus(HealthStatusManager.SERVICE_NAME_ALL_SERVICES, ServingStatus.NOT_SERVING);
    server.shutdown();
    server.awaitTermination();
  }
}
