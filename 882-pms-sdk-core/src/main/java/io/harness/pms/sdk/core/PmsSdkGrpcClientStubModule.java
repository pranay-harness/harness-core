package io.harness.pms.sdk.core;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcInProcessServer;
import io.harness.grpc.server.GrpcServer;
import io.harness.pms.contracts.plan.PmsServiceGrpc;
import io.harness.pms.contracts.service.EngineExpressionProtoServiceGrpc;
import io.harness.pms.contracts.service.InterruptProtoServiceGrpc;
import io.harness.pms.contracts.service.OutcomeProtoServiceGrpc;
import io.harness.pms.contracts.service.PmsExecutionServiceGrpc;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc;
import io.harness.pms.sdk.core.execution.expression.RemoteFunctorService;
import io.harness.pms.sdk.core.plan.creation.creators.PlanCreatorService;

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
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.grpc.services.HealthStatusManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLException;
import lombok.SneakyThrows;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsSdkGrpcClientStubModule extends AbstractModule {
  private final PmsSdkCoreConfig config;
  private static PmsSdkGrpcClientStubModule instance;
  private final String deployMode = System.getenv().get("DEPLOY_MODE");

  public static PmsSdkGrpcClientStubModule getInstance(PmsSdkCoreConfig config) {
    if (instance == null) {
      instance = new PmsSdkGrpcClientStubModule(config);
    }
    return instance;
  }

  protected PmsSdkGrpcClientStubModule(PmsSdkCoreConfig config) {
    this.config = config;
  }

  @SneakyThrows
  @Override
  protected void configure() {
    bind(PmsServiceGrpc.PmsServiceBlockingStub.class)
        .annotatedWith(Names.named(PmsSdkGrpcNamesUtil.getPmsClientStubAnnotation(config.getServiceName())))
        .toInstance(PmsServiceGrpc.newBlockingStub(getChannel()));
    bind(SweepingOutputServiceGrpc.SweepingOutputServiceBlockingStub.class)
        .annotatedWith(Names.named(PmsSdkGrpcNamesUtil.getPmsClientStubAnnotation(config.getServiceName())))
        .toInstance(SweepingOutputServiceGrpc.newBlockingStub(getChannel()));
    bind(InterruptProtoServiceGrpc.InterruptProtoServiceBlockingStub.class)
        .toInstance(InterruptProtoServiceGrpc.newBlockingStub(getChannel()));
    bind(OutcomeProtoServiceGrpc.OutcomeProtoServiceBlockingStub.class)
        .toInstance(OutcomeProtoServiceGrpc.newBlockingStub(getChannel()));
    bind(PmsExecutionServiceGrpc.PmsExecutionServiceBlockingStub.class)
        .toInstance(PmsExecutionServiceGrpc.newBlockingStub(getChannel()));
    bind(EngineExpressionProtoServiceGrpc.EngineExpressionProtoServiceBlockingStub.class)
        .toInstance(EngineExpressionProtoServiceGrpc.newBlockingStub(getChannel()));

    Multibinder<Service> serviceBinder = Multibinder.newSetBinder(binder(), Service.class, Names.named("pmsServices"));
    serviceBinder.addBinding().to(Key.get(Service.class, Names.named("pms-sdk-grpc-service")));
  }

  private Channel getChannel() throws SSLException {
    if (config.getSdkDeployMode() == SdkDeployMode.REMOTE_IN_PROCESS) {
      return InProcessChannelBuilder.forName(ModuleType.PMS.name().toLowerCase()).build();
    }

    GrpcClientConfig clientConfig = config.getGrpcClientConfig();
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
  @Named("pms-sdk-grpc-service")
  public Service pmsSdkGrpcService(HealthStatusManager healthStatusManager, PlanCreatorService planCreatorService,
      RemoteFunctorService remoteFunctorService) {
    Set<BindableService> cdServices = new HashSet<>();
    cdServices.add(healthStatusManager.getHealthService());
    cdServices.add(planCreatorService);
    cdServices.add(remoteFunctorService);
    if (config.getSdkDeployMode() == SdkDeployMode.REMOTE_IN_PROCESS) {
      return new GrpcInProcessServer("pmsSdkInternal", cdServices, Collections.emptySet(), healthStatusManager);
    }
    return new GrpcServer(
        config.getGrpcServerConfig().getConnectors().get(0), cdServices, Collections.emptySet(), healthStatusManager);
  }

  @Provides
  @Singleton
  @Named("pmsSDKServiceManager")
  public ServiceManager serviceManager(@Named("pmsServices") Set<Service> services) {
    return new ServiceManager(services);
  }
}
