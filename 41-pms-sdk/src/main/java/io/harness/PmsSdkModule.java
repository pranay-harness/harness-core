package io.harness;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import io.harness.grpc.server.GrpcServerConfig;
import io.harness.grpc.server.PmsSdkGrpcModule;
import io.harness.pms.sdk.creator.PlanCreatorProvider;
import io.harness.pms.sdk.creator.PlanCreatorService;
import io.harness.serializer.KryoSerializer;

public class PmsSdkModule extends AbstractModule {
  private final GrpcServerConfig grpcServerConfig;

  public PmsSdkModule(GrpcServerConfig grpcServerConfig) {
    this.grpcServerConfig = grpcServerConfig;
  }

  @Override
  protected void configure() {
    install(new PmsSdkGrpcModule(grpcServerConfig));
  }

  @Provides
  @Singleton
  public PlanCreatorService planCreatorService(KryoSerializer kryoSerializer, PlanCreatorProvider planCreatorProvider) {
    return new PlanCreatorService(kryoSerializer, planCreatorProvider);
  }
}
