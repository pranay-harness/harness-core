package io.harness.grpc.server;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.harness.PipelineServiceConfiguration;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.pms.plan.PlanCreationServiceGrpc;
import io.harness.pms.plan.PlanCreationServiceGrpc.PlanCreationServiceBlockingStub;

import java.util.ArrayList;
import java.util.List;

public class PipelineServiceGrpcModule extends AbstractModule {
  private final GrpcClientConfig cdGrpcClientConfig;
  private final GrpcClientConfig cvGrpcClientConfig;

  public PipelineServiceGrpcModule(PipelineServiceConfiguration config) {
    this.cdGrpcClientConfig = config.getCdGrpcClientConfig();
    this.cvGrpcClientConfig = config.getCvGrpcClientConfig();
  }

  @Override
  protected void configure() {}

  @Provides
  @Singleton
  @Named("cd-service-channel")
  public Channel cdServiceChannel() {
    return NettyChannelBuilder.forTarget(cdGrpcClientConfig.getTarget())
        .overrideAuthority(cdGrpcClientConfig.getAuthority())
        .usePlaintext()
        .build();
  }

  @Provides
  @Singleton
  @Named("cv-service-channel")
  public Channel cvServiceChannel() {
    return NettyChannelBuilder.forTarget(cvGrpcClientConfig.getTarget())
        .overrideAuthority(cvGrpcClientConfig.getAuthority())
        .usePlaintext()
        .build();
  }

  @Provides
  @Singleton
  @Named("cd-service")
  public PlanCreationServiceBlockingStub cdServiceBlockingStub(@Named("cd-service-channel") Channel channel) {
    return PlanCreationServiceGrpc.newBlockingStub(channel);
  }

  @Provides
  @Singleton
  @Named("cv-service")
  public PlanCreationServiceBlockingStub cvServiceBlockingStub(@Named("cv-service-channel") Channel channel) {
    return PlanCreationServiceGrpc.newBlockingStub(channel);
  }

  @Provides
  @Singleton
  public List<PlanCreationServiceBlockingStub> serviceBlockingStubs(
      @Named("cd-service") PlanCreationServiceBlockingStub cdService,
      @Named("cv-service") PlanCreationServiceBlockingStub cvService) {
    List<PlanCreationServiceBlockingStub> list = new ArrayList<>();
    list.add(cdService);
    list.add(cvService);
    return list;
  }
}
