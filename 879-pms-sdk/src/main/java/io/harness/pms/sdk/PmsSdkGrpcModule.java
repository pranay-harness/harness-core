package io.harness.pms.sdk;

import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServer;
import io.harness.pms.plan.PmsServiceGrpc;
import io.harness.pms.plan.PmsServiceGrpc.PmsServiceBlockingStub;
import io.harness.pms.sdk.core.plan.creation.creators.PlanCreatorService;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.services.HealthStatusManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PmsSdkGrpcModule extends AbstractModule {
  private final PmsSdkConfiguration config;
  private static PmsSdkGrpcModule instance;

  public static PmsSdkGrpcModule getInstance(PmsSdkConfiguration config) {
    if (instance == null) {
      instance = new PmsSdkGrpcModule(config);
    }
    return instance;
  }

  private PmsSdkGrpcModule(PmsSdkConfiguration config) {
    this.config = config;
  }

  @Override
  protected void configure() {
    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    serviceBinder.addBinding().to(Key.get(Service.class, Names.named("pms-sdk-grpc-service")));
  }

  @Provides
  @Singleton
  @Named("pms-sdk-grpc-service")
  public Service pmsSdkGrpcService(HealthStatusManager healthStatusManager, PlanCreatorService planCreatorService) {
    Set<BindableService> cdServices = new HashSet<>();
    cdServices.add(healthStatusManager.getHealthService());
    cdServices.add(planCreatorService);
    return new GrpcServer(
        config.getGrpcServerConfig().getConnectors().get(0), cdServices, Collections.emptySet(), healthStatusManager);
  }

  @Provides
  @Singleton
  public PmsServiceBlockingStub pmsGrpcClient(
      HealthStatusManager healthStatusManager, PlanCreatorService planCreatorService) {
    GrpcClientConfig clientConfig = config.getPmsGrpcClientConfig();
    Channel channel = NettyChannelBuilder.forTarget(clientConfig.getTarget())
                          .overrideAuthority(clientConfig.getAuthority())
                          .usePlaintext()
                          .build();
    return PmsServiceGrpc.newBlockingStub(channel);
  }
}
