package io.harness.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * This class is currently for testing purpose only.
 */
@Slf4j
@Singleton
public class GrpcServer {
  private final GrpcServerConfig grpcServerConfig;
  private final BindableService service;
  private Server plainServer;
  private Server tlsServer;

  @Inject
  public GrpcServer(GrpcServerConfig grpcServerConfig, BindableService service) {
    this.grpcServerConfig = grpcServerConfig;
    this.service = service;
  }

  public void initialize() throws IOException {
    plainServer = ServerBuilder.forPort(grpcServerConfig.getPlainTextPort()).addService(service).build();
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
