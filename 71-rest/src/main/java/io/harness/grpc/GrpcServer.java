package io.harness.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Set;

/**
 * This class is currently for testing purpose only.
 */
@Slf4j
@Singleton
public class GrpcServer {
  private final GrpcServerConfig grpcServerConfig;
  private final Set<BindableService> services;
  private Server plainServer;
  private Server tlsServer;

  @Inject
  public GrpcServer(GrpcServerConfig grpcServerConfig, Set<BindableService> services) {
    this.grpcServerConfig = grpcServerConfig;
    this.services = services;
  }

  public void initialize() throws IOException {
    ServerBuilder serverBuilder = ServerBuilder.forPort(grpcServerConfig.getPlainTextPort());
    for (BindableService service : services) {
      serverBuilder.addService(service);
    }
    plainServer = serverBuilder.build();
    plainServer.start();

    /*File certChain = new File(grpcServerConfig.getCertFile());
    File privateKey = new File(grpcServerConfig.getKeyFile());
    tlsServer = ServerBuilder.forPort(grpcServerConfig.getTlsPort())
                    .useTransportSecurity(certChain, privateKey)
                    .addService(service)
                    .build();
    tlsServer.start();*/

    logger.info("gRPC Server started at port " + Integer.toString(grpcServerConfig.getPlainTextPort()));
  }

  public void stop() {
    plainServer.shutdown();
  }

  public void awaitTermination() throws InterruptedException {
    if (plainServer != null) {
      plainServer.awaitTermination();
      tlsServer.awaitTermination();
    }
  }
}
