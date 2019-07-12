package software.wings.app;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import io.harness.event.grpc.EventPublishServer;
import io.harness.event.grpc.GrpcServerConfig;
import io.harness.event.grpc.auth.DelegateAuthServerInterceptor;

public class GrpcServerModule extends AbstractModule {
  private final GrpcServerConfig grpcServerConfig;

  public GrpcServerModule(GrpcServerConfig grpcServerConfig) {
    this.grpcServerConfig = grpcServerConfig;
  }

  @Override
  protected void configure() {}

  @Provides
  public GrpcServer grpcServer(EventPublishServer service, DelegateAuthServerInterceptor authInterceptor) {
    return new GrpcServer(service, authInterceptor, grpcServerConfig);
  }
}