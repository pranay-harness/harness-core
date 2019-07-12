package software.wings.app;

import com.google.inject.Inject;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.harness.event.grpc.EventPublishServer;
import io.harness.event.grpc.GrpcServerConfig;
import io.harness.event.grpc.auth.DelegateAuthServerInterceptor;

import java.io.File;
import java.io.IOException;

public class GrpcServer {
  private final DelegateAuthServerInterceptor authInterceptor;
  private final GrpcServerConfig grpcServerConfig;
  private final EventPublishServer service;

  @Inject
  public GrpcServer(
      EventPublishServer service, DelegateAuthServerInterceptor authInterceptor, GrpcServerConfig grpcServerConfig) {
    this.authInterceptor = authInterceptor;
    this.grpcServerConfig = grpcServerConfig;
    this.service = service;
  }

  public void initalize() throws IOException {
    Server plainServer = ServerBuilder.forPort(grpcServerConfig.getPlainTextPort())
                             .intercept(authInterceptor)
                             .addService(service)
                             .build();
    plainServer.start();

    File certChain = new File(grpcServerConfig.getCertFile());
    File privateKey = new File(grpcServerConfig.getKeyFile());
    Server tlsServer = ServerBuilder.forPort(grpcServerConfig.getTlsPort())
                           .useTransportSecurity(certChain, privateKey)
                           .intercept(authInterceptor)
                           .addService(service)
                           .build();
    tlsServer.start();
  }
}
