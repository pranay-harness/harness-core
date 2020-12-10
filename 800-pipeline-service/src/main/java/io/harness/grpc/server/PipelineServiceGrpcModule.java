package io.harness.grpc.server;

import io.harness.PipelineServiceConfiguration;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.pms.plan.PlanCreationServiceGrpc;
import io.harness.pms.plan.PlanCreationServiceGrpc.PlanCreationServiceBlockingStub;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.sdk.service.outputs.SweepingOutputServiceImpl;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PipelineServiceGrpcModule extends AbstractModule {
  private static PipelineServiceGrpcModule instance;

  public static PipelineServiceGrpcModule getInstance() {
    if (instance == null) {
      instance = new PipelineServiceGrpcModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class);
    serviceBinder.addBinding().to(Key.get(Service.class, Names.named("pms-grpc-service")));
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }

  @Provides
  @Singleton
  public Map<String, PlanCreationServiceBlockingStub> grpcClients(PipelineServiceConfiguration configuration) {
    Map<String, PlanCreationServiceBlockingStub> map = new HashMap<>();
    for (Map.Entry<String, GrpcClientConfig> entry : configuration.getGrpcClientConfigs().entrySet()) {
      Channel channel = NettyChannelBuilder.forTarget(entry.getValue().getTarget())
                            .overrideAuthority(entry.getValue().getAuthority())
                            .usePlaintext()
                            .build();
      map.put(entry.getKey(), PlanCreationServiceGrpc.newBlockingStub(channel));
    }
    return map;
  }

  @Provides
  @Singleton
  @Named("pms-grpc-service")
  public Service pmsGrpcService(PipelineServiceConfiguration configuration, HealthStatusManager healthStatusManager,
      PmsSdkInstanceService pmsSdkInstanceService, SweepingOutputServiceImpl sweepingOutputService) {
    Set<BindableService> cdServices = new HashSet<>();
    cdServices.add(healthStatusManager.getHealthService());
    cdServices.add(pmsSdkInstanceService);
    cdServices.add(sweepingOutputService);
    return new GrpcServer(configuration.getGrpcServerConfig().getConnectors().get(0), cdServices,
        Collections.emptySet(), healthStatusManager);
  }
}
