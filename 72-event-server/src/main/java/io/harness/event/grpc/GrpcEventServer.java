package io.harness.event.grpc;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.harness.event.app.EventServiceConfig;
import io.harness.exception.WingsException;
import io.harness.grpc.auth.DelegateAuthServerInterceptor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

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

  public void initialize() {
    // Ingress will forward to plain server in production.
    try {
      if (eventServiceConfig.getPlainTextPort() > 0) {
        plainServer = ServerBuilder.forPort(eventServiceConfig.getPlainTextPort())
                          .intercept(authInterceptor)
                          .addService(service)
                          .build();
        plainServer.start();
      }

      // TLS server for local testing. Won't be exposed by container configuration in production.
      if (eventServiceConfig.getSecurePort() > 0) {
        File certChain = new File(eventServiceConfig.getCertFilePath());
        File privateKey = new File(eventServiceConfig.getKeyFilePath());
        Preconditions.checkState(certChain.exists() && privateKey.exists());
        tlsServer = ServerBuilder.forPort(eventServiceConfig.getSecurePort())
                        .useTransportSecurity(certChain, privateKey)
                        .intercept(authInterceptor)
                        .addService(service)
                        .build();
        tlsServer.start();
      }
    } catch (Exception e) {
      logger.error("Error starting server");
      throw new WingsException(e);
    }
  }

  public void awaitTermination() throws InterruptedException {
    if (plainServer != null) {
      plainServer.awaitTermination();
    }
    if (tlsServer != null) {
      tlsServer.awaitTermination();
    }
  }

  public void shutdown() {
    logger.debug("Shutting down...");
    if (plainServer != null) {
      plainServer.shutdown();
    }
    if (tlsServer != null) {
      tlsServer.shutdown();
    }
  }
}
