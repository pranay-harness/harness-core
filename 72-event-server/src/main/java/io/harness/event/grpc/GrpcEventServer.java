package io.harness.event.grpc;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.harness.event.app.EventServiceConfig;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
@Singleton
public class GrpcEventServer {
  private final DelegateAuthServerInterceptor authInterceptor;
  private final EventServiceConfig eventServiceConfig;
  private final EventPublisherServerImpl service;
  private Server plainServer;
  private Server tlsServer;

  @Inject
  public GrpcEventServer(EventPublisherServerImpl service, DelegateAuthServerInterceptor authInterceptor,
      EventServiceConfig eventServiceConfig) {
    this.authInterceptor = authInterceptor;
    this.eventServiceConfig = eventServiceConfig;
    this.service = service;
  }

  @Inject
  public void initialize() throws IOException {
    // Ingress will forward to plain server in production.
    plainServer = ServerBuilder.forPort(eventServiceConfig.getPlainTextPort())
                      .intercept(authInterceptor)
                      .addService(service)
                      .build();
    plainServer.start();

    // TLS server for local testing. Won't be exposed by container configuration in production.
    File certChain = new File(eventServiceConfig.getCertFilePath());
    File privateKey = new File(eventServiceConfig.getKeyFilePath());
    tlsServer = ServerBuilder.forPort(eventServiceConfig.getSecurePort())
                    .useTransportSecurity(certChain, privateKey)
                    .intercept(authInterceptor)
                    .addService(service)
                    .build();
    tlsServer.start();
  }

  public void awaitTermination() throws InterruptedException {
    plainServer.awaitTermination();
    tlsServer.awaitTermination();
  }

  public void shutdown() {
    logger.debug("Shutting down...");
    plainServer.shutdown();
    tlsServer.shutdown();
  }
}
