package io.harness.delegate.app;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.cache.CacheConfig;
import io.harness.configuration.DeployMode;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.lock.DistributedLockImplementation;
import io.harness.mongo.MongoConfig;
import io.harness.redis.RedisConfig;
import io.harness.stream.AtmosphereBroadcaster;

import software.wings.beans.HttpMethod;
import software.wings.jre.JreConfig;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import io.dropwizard.Configuration;
import io.dropwizard.bundles.assets.AssetsBundleConfiguration;
import io.dropwizard.bundles.assets.AssetsConfiguration;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.logging.FileAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.request.logging.RequestLogFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Used to load all the delegate mgr configuration.
 */
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
@OwnedBy(DEL)
public class DelegateServiceConfig extends Configuration implements AssetsBundleConfiguration {
  @JsonProperty
  private AssetsConfiguration assetsConfiguration =
      AssetsConfiguration.builder()
          .mimeTypes(of("js", "application/json; charset=UTF-8", "zip", "application/zip"))
          .build();

  @JsonProperty("redisLockConfig") private RedisConfig redisLockConfig;
  @JsonProperty("redisAtmosphereConfig") private RedisConfig redisAtmosphereConfig;
  @JsonProperty("currentJre") private String currentJre;
  @JsonProperty("jreConfigs") private Map<String, JreConfig> jreConfigs;
  @JsonProperty("atmosphereBroadcaster") private AtmosphereBroadcaster atmosphereBroadcaster;
  @JsonProperty("cacheConfig") private CacheConfig cacheConfig;
  @JsonProperty("delegateServiceUrl") private String delegateServiceUrl;
  @JsonProperty("eventsFramework") private EventsFrameworkConfiguration eventsFrameworkConfiguration;
  @JsonProperty("distributedLockImplementation") private DistributedLockImplementation distributedLockImplementation;
  @JsonProperty(defaultValue = "KUBERNETES") private DeployMode deployMode = DeployMode.KUBERNETES;
  @JsonProperty("grpcServerConfig") private GrpcServerConfig grpcServerConfig;
  @JsonProperty("grpcServerClassicConfig") private GrpcServerConfig grpcServerClassicConfig;
  @JsonProperty("delegateServiceSecret") private String delegateServiceSecret;
  @JsonProperty("mongo") private MongoConfig mongoConnectionFactory = MongoConfig.builder().build();

  private int applicationPort;
  private boolean sslEnabled;

  private static final String IS_OPTION_HEAD_HTTP_METHOD_BLOCKED = "IS_OPTION_HEAD_REQUEST_METHOD_BLOCKED";

  /**
   * Instantiates a new Main configuration.
   */
  public DelegateServiceConfig() {
    DefaultServerFactory defaultServerFactory = new DefaultServerFactory();
    defaultServerFactory.setJerseyRootPath("/delegate");
    defaultServerFactory.setRegisterDefaultExceptionMappers(false);
    defaultServerFactory.setAdminContextPath("/admin");
    defaultServerFactory.setAdminConnectors(singletonList(getDefaultAdminConnectorFactory()));
    defaultServerFactory.setApplicationConnectors(singletonList(getDefaultApplicationConnectorFactory()));
    defaultServerFactory.setRequestLogFactory(getDefaultlogbackAccessRequestLogFactory());
    defaultServerFactory.setAllowedMethods(getAllowedMethods());
    super.setServerFactory(defaultServerFactory);
  }

  @Override
  public void setServerFactory(ServerFactory factory) {
    DefaultServerFactory defaultServerFactory = (DefaultServerFactory) factory;
    ((DefaultServerFactory) getServerFactory())
        .setApplicationConnectors(defaultServerFactory.getApplicationConnectors());
    ((DefaultServerFactory) getServerFactory()).setAdminConnectors(defaultServerFactory.getAdminConnectors());
    ((DefaultServerFactory) getServerFactory()).setRequestLogFactory(defaultServerFactory.getRequestLogFactory());
    ((DefaultServerFactory) getServerFactory()).setMaxThreads(defaultServerFactory.getMaxThreads());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AssetsConfiguration getAssetsConfiguration() {
    return assetsConfiguration;
  }

  private ConnectorFactory getDefaultAdminConnectorFactory() {
    final HttpConnectorFactory factory = new HttpConnectorFactory();
    factory.setPort(9091);
    return factory;
  }

  private ConnectorFactory getDefaultApplicationConnectorFactory() {
    final HttpConnectorFactory factory = new HttpConnectorFactory();
    factory.setPort(9090);
    return factory;
  }

  private Set<String> getAllowedMethods() {
    if (System.getenv(IS_OPTION_HEAD_HTTP_METHOD_BLOCKED) != null
        && Boolean.parseBoolean(System.getenv(IS_OPTION_HEAD_HTTP_METHOD_BLOCKED))) {
      return new HashSet<>(Arrays.asList(HttpMethod.GET.name(), HttpMethod.PUT.name(), HttpMethod.POST.name(),
          HttpMethod.PATCH.name(), HttpMethod.DELETE.name()));
    }
    return new HashSet<>(Arrays.asList(HttpMethod.OPTIONS.name(), HttpMethod.POST.name(), HttpMethod.GET.name(),
        HttpMethod.PUT.name(), HttpMethod.HEAD.name(), HttpMethod.PATCH.name(), HttpMethod.DELETE.name()));
  }

  private RequestLogFactory getDefaultlogbackAccessRequestLogFactory() {
    LogbackAccessRequestLogFactory logbackAccessRequestLogFactory = new LogbackAccessRequestLogFactory();
    FileAppenderFactory<IAccessEvent> fileAppenderFactory = new FileAppenderFactory<>();
    fileAppenderFactory.setArchive(true);
    fileAppenderFactory.setCurrentLogFilename("access.log");
    fileAppenderFactory.setThreshold(Level.ALL.toString());
    fileAppenderFactory.setArchivedLogFilenamePattern("access.%d.log.gz");
    fileAppenderFactory.setArchivedFileCount(14);
    logbackAccessRequestLogFactory.setAppenders(ImmutableList.of(fileAppenderFactory));
    return logbackAccessRequestLogFactory;
  }
}
