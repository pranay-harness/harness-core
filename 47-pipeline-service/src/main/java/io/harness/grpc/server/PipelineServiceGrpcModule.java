package io.harness.grpc.server;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;

import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.harness.PipelineServiceConfiguration;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.pms.plan.PlanCreationServiceGrpc;
import io.harness.pms.plan.PlanCreationServiceGrpc.PlanCreationServiceBlockingStub;

import java.util.Collections;
import java.util.Map;

public class PipelineServiceGrpcModule extends AbstractModule {
  private final Map<String, GrpcClientConfig> grpcClientConfigs;

  public PipelineServiceGrpcModule(PipelineServiceConfiguration config) {
    this.grpcClientConfigs =
        config.getGrpcClientConfigs() == null ? Collections.emptyMap() : config.getGrpcClientConfigs();
  }

  @Override
  protected void configure() {
    MapBinder<String, PlanCreationServiceBlockingStub> mapBinder =
        MapBinder.newMapBinder(binder(), String.class, PlanCreationServiceBlockingStub.class);
    for (Map.Entry<String, GrpcClientConfig> entry : grpcClientConfigs.entrySet()) {
      Channel channel = NettyChannelBuilder.forTarget(entry.getValue().getTarget())
                            .overrideAuthority(entry.getValue().getAuthority())
                            .usePlaintext()
                            .build();
      mapBinder.addBinding(entry.getKey()).toInstance(PlanCreationServiceGrpc.newBlockingStub(channel));
    }
  }
}
