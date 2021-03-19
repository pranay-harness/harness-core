package io.harness.grpc.server;

import io.harness.PipelineServiceConfiguration;
import io.harness.engine.executions.node.PmsNodeExecutionGrpcSevice;
import io.harness.engine.interrupts.InterruptGrpcService;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.pms.contracts.plan.PlanCreationServiceGrpc;
import io.harness.pms.contracts.plan.PlanCreationServiceGrpc.PlanCreationServiceBlockingStub;
import io.harness.pms.plan.execution.data.service.expressions.EngineExpressionGrpcServiceImpl;
import io.harness.pms.plan.execution.data.service.outcome.OutcomeServiceGrpcServerImpl;
import io.harness.pms.plan.execution.data.service.outputs.SweepingOutputServiceImpl;
import io.harness.pms.sdk.PmsSdkInstanceService;
import io.harness.pms.sdk.service.execution.PmsExecutionGrpcService;
import io.harness.pms.utils.PmsConstants;

import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.grpc.services.HealthStatusManager;
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
    Multibinder<BindableService> serviceBinderInProcess =
        Multibinder.newSetBinder(binder(), BindableService.class, Names.named("in_process_services"));
    Multibinder<BindableService> serviceBinderRemote =
        Multibinder.newSetBinder(binder(), BindableService.class, Names.named("out_process_services"));
    serviceBinderInProcess.addBinding().to(PmsSdkInstanceService.class);
    serviceBinderInProcess.addBinding().to(PmsNodeExecutionGrpcSevice.class);
    serviceBinderInProcess.addBinding().to(PmsExecutionGrpcService.class);
    serviceBinderInProcess.addBinding().to(SweepingOutputServiceImpl.class);
    serviceBinderInProcess.addBinding().to(OutcomeServiceGrpcServerImpl.class);
    serviceBinderInProcess.addBinding().to(EngineExpressionGrpcServiceImpl.class);
    serviceBinderInProcess.addBinding().to(InterruptGrpcService.class);

    serviceBinderRemote.addBinding().to(PmsSdkInstanceService.class);
    serviceBinderRemote.addBinding().to(PmsNodeExecutionGrpcSevice.class);
    serviceBinderRemote.addBinding().to(PmsExecutionGrpcService.class);
    serviceBinderRemote.addBinding().to(SweepingOutputServiceImpl.class);
    serviceBinderRemote.addBinding().to(OutcomeServiceGrpcServerImpl.class);
    serviceBinderRemote.addBinding().to(EngineExpressionGrpcServiceImpl.class);
    serviceBinderRemote.addBinding().to(InterruptGrpcService.class);
  }

  @Provides
  @Singleton
  public ServiceManager serviceManager(Set<Service> services) {
    return new ServiceManager(services);
  }

  @Provides
  @Singleton
  public Map<String, PlanCreationServiceBlockingStub> grpcClients(PipelineServiceConfiguration configuration)
      throws SSLException {
    Map<String, PlanCreationServiceBlockingStub> map = new HashMap<>();
    map.put(PmsConstants.INTERNAL_SERVICE_NAME,
        PlanCreationServiceGrpc.newBlockingStub(
            InProcessChannelBuilder.forName(GrpcServerConstants.IN_PROCESS_SERVICES).build()));
    for (Map.Entry<String, GrpcClientConfig> entry : configuration.getGrpcClientConfigs().entrySet()) {
      map.put(entry.getKey(), PlanCreationServiceGrpc.newBlockingStub(getChannel(entry.getValue())));
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
  private Set<BindableService> bindableServices(HealthStatusManager healthStatusManager,
      PmsSdkInstanceService pmsSdkInstanceService, PmsNodeExecutionGrpcSevice pmsNodeExecutionGrpcSevice,
      PmsExecutionGrpcService pmsExecutionGrpcService, SweepingOutputServiceImpl sweepingOutputService,
      OutcomeServiceGrpcServerImpl outcomeServiceGrpcServer,
      EngineExpressionGrpcServiceImpl engineExpressionGrpcService, InterruptGrpcService interruptGrpcService) {
    Set<BindableService> services = new HashSet<>();
    services.add(healthStatusManager.getHealthService());
    services.add(pmsSdkInstanceService);
    services.add(pmsNodeExecutionGrpcSevice);
    services.add(pmsExecutionGrpcService);
    services.add(sweepingOutputService);
    services.add(outcomeServiceGrpcServer);
    services.add(engineExpressionGrpcService);
    services.add(interruptGrpcService);
    return services;
  }
}
