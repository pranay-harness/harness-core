package io.harness.pms.sdk;

import io.harness.AuthorizationServiceHeader;
import io.harness.grpc.auth.GrpcAuthServiceInterceptor;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcInProcessServer;
import io.harness.grpc.server.GrpcInProcessServerModule;
import io.harness.grpc.server.GrpcServerModule;
import io.harness.pms.contracts.plan.PmsServiceGrpc;
import io.harness.pms.contracts.plan.PmsServiceGrpc.PmsServiceBlockingStub;
import io.harness.pms.contracts.service.EngineExpressionProtoServiceGrpc;
import io.harness.pms.contracts.service.EngineExpressionProtoServiceGrpc.EngineExpressionProtoServiceBlockingStub;
import io.harness.pms.contracts.service.InterruptProtoServiceGrpc;
import io.harness.pms.contracts.service.InterruptProtoServiceGrpc.InterruptProtoServiceBlockingStub;
import io.harness.pms.contracts.service.OutcomeProtoServiceGrpc;
import io.harness.pms.contracts.service.OutcomeProtoServiceGrpc.OutcomeProtoServiceBlockingStub;
import io.harness.pms.contracts.service.PmsExecutionServiceGrpc;
import io.harness.pms.contracts.service.PmsExecutionServiceGrpc.PmsExecutionServiceBlockingStub;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc;
import io.harness.pms.contracts.service.SweepingOutputServiceGrpc.SweepingOutputServiceBlockingStub;
import io.harness.pms.sdk.PmsSdkConfiguration.DeployMode;
import io.harness.pms.sdk.core.plan.creation.creators.PlanCreatorService;
import io.harness.pms.utils.PmsConstants;
import io.harness.version.VersionInfo;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.grpc.BindableService;
import io.grpc.Channel;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.internal.GrpcUtil;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.SSLException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PmsSdkGrpcModule extends AbstractModule {
  private final PmsSdkConfiguration config;
  private static PmsSdkGrpcModule instance;
  private final String deployMode = System.getenv().get("DEPLOY_MODE");

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
    Multibinder<BindableService> bindableServiceMultibinder =
        Multibinder.newSetBinder(binder(), BindableService.class, Names.named("sdkServices"));
    Multibinder<ServerInterceptor> serverInterceptorMultibinder =
        Multibinder.newSetBinder(binder(), ServerInterceptor.class);
    serverInterceptorMultibinder.addBinding().to(GrpcAuthServiceInterceptor.class);
    bindableServiceMultibinder.addBinding().to(PlanCreatorService.class);
    if (config.getDeploymentMode() == DeployMode.REMOTE_IN_PROCESS) {
      install(new GrpcInProcessServerModule(
          getProvider(Key.get(new TypeLiteral<Set<BindableService>>() {}, Names.named("sdkServices"))),
          getProvider(Key.get(new TypeLiteral<Set<ServerInterceptor>>() {})), "pmsServices"));
    } else {
      install(new GrpcServerModule(config.getGrpcServerConfig().getConnectors(),
          getProvider(Key.get(new TypeLiteral<Set<BindableService>>() {}, Names.named("sdkServices"))),
          getProvider(Key.get(new TypeLiteral<Set<ServerInterceptor>>() {})), "pmsServices"));
    }
  }

  private String computeAuthority(String authority, VersionInfo versionInfo) {
    String defaultAuthority = "default-authority.harness.io";
    String authorityToUse;
    if (!isValidAuthority(authority)) {
      log.info("Authority in config {} is invalid. Using default value {}", authority, defaultAuthority);
      authorityToUse = defaultAuthority;
    } else if (!("ONPREM".equals(deployMode) || "KUBERNETES_ONPREM".equals(deployMode))) {
      String versionPrefix = "v-" + versionInfo.getVersion().replace('.', '-') + "-";
      String versionedAuthority = versionPrefix + authority;
      if (isValidAuthority(versionedAuthority)) {
        log.info("Using versioned authority: {}", versionedAuthority);
        authorityToUse = versionedAuthority;
      } else {
        log.info("Versioned authority {} is invalid. Using non-versioned", versionedAuthority);
        authorityToUse = authority;
      }
    } else {
      log.info("Deploy Mode is {}. Using non-versioned authority", deployMode);
      authorityToUse = authority;
    }
    return authorityToUse;
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

  private Channel getChannel() throws SSLException {
    if (config.getDeploymentMode() == DeployMode.REMOTE_IN_PROCESS) {
      return InProcessChannelBuilder.forName(PmsConstants.INTERNAL_SERVICE_NAME).build();
    }

    GrpcClientConfig clientConfig = config.getPmsGrpcClientConfig();
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
  public PmsServiceBlockingStub pmsGrpcClient() throws SSLException {
    return PmsServiceGrpc.newBlockingStub(getChannel());
  }

  @Provides
  @Singleton
  public SweepingOutputServiceBlockingStub sweepingOutputGrpcClient() throws SSLException {
    return SweepingOutputServiceGrpc.newBlockingStub(getChannel());
  }

  @Provides
  @Singleton
  public InterruptProtoServiceBlockingStub interruptProtoGrpcClient() throws SSLException {
    return InterruptProtoServiceGrpc.newBlockingStub(getChannel());
  }

  @Provides
  @Singleton
  public OutcomeProtoServiceBlockingStub outcomeGrpcClient() throws SSLException {
    return OutcomeProtoServiceGrpc.newBlockingStub(getChannel());
  }

  @Provides
  @Singleton
  public PmsExecutionServiceBlockingStub executionServiceGrpcClient() throws SSLException {
    return PmsExecutionServiceGrpc.newBlockingStub(getChannel());
  }

  @Provides
  @Singleton
  public EngineExpressionProtoServiceBlockingStub engineExpressionGrpcClient() throws SSLException {
    return EngineExpressionProtoServiceGrpc.newBlockingStub(getChannel());
  }

  @Provides
  @Singleton
  @Named("grpc-services-with-auth")
  public Set<String> grpcServicesWithAuth() throws SSLException {
    return Sets.newHashSet("io.harness.pms.contracts.plan.PlanCreationService");
  }

  @Provides
  @Singleton
  @Named("serviceAuthTokens")
  public Map<String, String> serviceIdToServiceToken() throws SSLException {
    Map<String, String> serviceToSecretMapping = new HashMap<>();
    serviceToSecretMapping.put(AuthorizationServiceHeader.DEFAULT.getServiceId(), config.getPipelineServiceSecret());
    return serviceToSecretMapping;
  }

  @Provides
  @Singleton
  @Named("pmsSDKServiceManager")
  public ServiceManager serviceManager(@Named("pmsServices") Set<Service> services) {
    return new ServiceManager(services);
  }
}
