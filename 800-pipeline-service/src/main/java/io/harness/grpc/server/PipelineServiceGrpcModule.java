package io.harness.grpc.server;

import io.harness.PipelineServiceConfiguration;
import io.harness.PipelineServiceGrpcClientConfig;
import io.harness.engine.interrupts.InterruptGrpcService;
import io.harness.grpc.auth.ServiceAuthCallCredentials;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.pms.contracts.plan.PlanCreationServiceGrpc;
import io.harness.pms.contracts.plan.PlanCreationServiceGrpc.PlanCreationServiceBlockingStub;
import io.harness.pms.plan.execution.data.service.expressions.EngineExpressionGrpcServiceImpl;
import io.harness.pms.plan.execution.data.service.outcome.OutcomeServiceGrpcServerImpl;
import io.harness.pms.plan.execution.data.service.outputs.SweepingOutputServiceImpl;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.sdk.service.execution.PmsExecutionGrpcService;
import io.harness.pms.utils.PmsConstants;
import io.harness.security.ServiceTokenGenerator;

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
import io.grpc.CallCredentials;
import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.grpc.services.HealthStatusManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PipelineServiceGrpcModule extends AbstractModule {
  private static PipelineServiceGrpcModule instance;
  private final String deployMode = System.getenv().get("DEPLOY_MODE");

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
    serviceBinder.addBinding().to(Key.get(Service.class, Names.named("pms-grpc-internal-service")));
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }

  @Provides
  @Singleton
  public Map<String, PlanCreationServiceBlockingStub> grpcClients(PipelineServiceConfiguration configuration,
      @Named("sdk-call-credentials") Map<String, CallCredentials> credentialsMap) throws SSLException {
    Map<String, PlanCreationServiceBlockingStub> map = new HashMap<>();
    map.put(PmsConstants.INTERNAL_SERVICE_NAME,
        PlanCreationServiceGrpc.newBlockingStub(InProcessChannelBuilder.forName("pmsSdkInternal").build())
            .withCallCredentials(credentialsMap.get("cd")));

    for (Map.Entry<String, GrpcClientConfig> entry : configuration.getGrpcClientConfigs().entrySet()) {
      map.put(entry.getKey(),
          PlanCreationServiceGrpc.newBlockingStub(getChannel(entry.getValue()))
              .withCallCredentials(credentialsMap.get(entry.getKey())));
    }
    return map;
  }

  private static boolean isValidAuthority(String authority) {
    try {
      GrpcUtil.checkAuthority(authority);
    } catch (Exception ignore) {
      log.error("Exception occurred when checking for valid authority", ignore);
      return false;
    }
    return true;
  }

  private Channel getChannel(GrpcClientConfig clientConfig) throws SSLException {
    String authorityToUse = clientConfig.getAuthority();
    Channel channel;

    if ("ONPREM".equals(deployMode) || "KUBERNETES_ONPREM".equals(deployMode)) {
      channel = NettyChannelBuilder.forTarget(clientConfig.getTarget())
                    .overrideAuthority(authorityToUse)
                    .usePlaintext()
                    .maxInboundMessageSize(GrpcInProcessServer.GRPC_MAXIMUM_MESSAGE_SIZE)
                    .build();
    } else {
      SslContext sslContext = GrpcSslContexts.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
      channel = NettyChannelBuilder.forTarget(clientConfig.getTarget())
                    .overrideAuthority(authorityToUse)
                    .sslContext(sslContext)
                    .maxInboundMessageSize(GrpcInProcessServer.GRPC_MAXIMUM_MESSAGE_SIZE)
                    .build();
    }

    return channel;
  }

  @Provides
  @Singleton
  @Named("pms-grpc-service")
  public Service pmsGrpcService(PipelineServiceConfiguration configuration, HealthStatusManager healthStatusManager,
      Set<BindableService> services) {
    return new GrpcServer(configuration.getGrpcServerConfig().getConnectors().get(0), services, Collections.emptySet(),
        healthStatusManager);
  }

  @Provides
  @Singleton
  @Named("pms-grpc-internal-service")
  public Service pmsGrpcInternalService(HealthStatusManager healthStatusManager, Set<BindableService> services) {
    return new GrpcInProcessServer(
        PmsConstants.INTERNAL_SERVICE_NAME, services, Collections.emptySet(), healthStatusManager);
  }

  @Provides
  private Set<BindableService> bindableServices(HealthStatusManager healthStatusManager,
      PmsSdkInstanceService pmsSdkInstanceService, PmsExecutionGrpcService pmsExecutionGrpcService,
      SweepingOutputServiceImpl sweepingOutputService, OutcomeServiceGrpcServerImpl outcomeServiceGrpcServer,
      EngineExpressionGrpcServiceImpl engineExpressionGrpcService, InterruptGrpcService interruptGrpcService) {
    Set<BindableService> services = new HashSet<>();
    services.add(healthStatusManager.getHealthService());
    services.add(pmsSdkInstanceService);
    services.add(pmsExecutionGrpcService);
    services.add(sweepingOutputService);
    services.add(outcomeServiceGrpcServer);
    services.add(engineExpressionGrpcService);
    services.add(interruptGrpcService);
    return services;
  }

  @Named("sdk-call-credentials")
  @Provides
  @Singleton
  Map<String, CallCredentials> callCredentials(PipelineServiceConfiguration delegateConfiguration) {
    Map<String, CallCredentials> map = new HashMap<>();
    for (Map.Entry<String, PipelineServiceGrpcClientConfig> entry :
        delegateConfiguration.getGrpcClientServiceSecrets().entrySet()) {
      map.put(entry.getKey(),
          new ServiceAuthCallCredentials(
              entry.getValue().getSecret(), new ServiceTokenGenerator(), "pipeline-service"));
    }
    return map;
  }
}
