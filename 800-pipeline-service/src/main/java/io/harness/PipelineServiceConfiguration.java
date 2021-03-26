package io.harness;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.mongo.MongoConfig;
import io.harness.notification.NotificationClientConfiguration;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.scm.ScmConnectionConfig;
import io.harness.yaml.schema.client.config.YamlSchemaClientConfig;

import ch.qos.logback.access.spi.IAccessEvent;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;
import io.dropwizard.Configuration;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.logging.FileAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.request.logging.RequestLogFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import javax.ws.rs.Path;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.reflections.Reflections;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class PipelineServiceConfiguration extends Configuration {
  public static final String RESOURCE_PACKAGE = "io.harness.pms";
  public static final String NG_TRIGGER_RESOURCE_PACKAGE = "io.harness.ngtriggers";
  public static final String FILTER_PACKAGE = "io.harness.filter";

  @JsonProperty("swagger") private SwaggerBundleConfiguration swaggerBundleConfiguration;
  @JsonProperty("mongo") private MongoConfig mongoConfig;
  @JsonProperty("grpcServerConfig") private GrpcServerConfig grpcServerConfig;
  @JsonProperty("grpcClientConfigs") private Map<String, GrpcClientConfig> grpcClientConfigs;
  @JsonProperty("ngManagerServiceHttpClientConfig") private ServiceHttpClientConfig ngManagerServiceHttpClientConfig;
  @JsonProperty("ngManagerServiceSecret") private String ngManagerServiceSecret;
  @JsonProperty("jwtAuthSecret") private String jwtAuthSecret;
  @JsonProperty("jwtIdentityServiceSecret") private String jwtIdentityServiceSecret;
  @Builder.Default @JsonProperty("allowedOrigins") private List<String> allowedOrigins = new ArrayList<>();
  @JsonProperty("notificationClient") private NotificationClientConfiguration notificationClientConfiguration;
  @JsonProperty("eventsFramework") private EventsFrameworkConfiguration eventsFrameworkConfiguration;
  @JsonProperty("pipelineServiceBaseUrl") private String pipelineServiceBaseUrl;
  @JsonProperty("enableAuth") private boolean enableAuth;
  @JsonProperty("yamlSchemaClientConfig") private YamlSchemaClientConfig yamlSchemaClientConfig;

  private String managerServiceSecret;
  private String managerTarget;
  private String managerAuthority;
  private ScmConnectionConfig scmConnectionConfig;
  private ServiceHttpClientConfig managerClientConfig;

  public PipelineServiceConfiguration() {
    DefaultServerFactory defaultServerFactory = new DefaultServerFactory();
    defaultServerFactory.setJerseyRootPath("/api");
    defaultServerFactory.setRegisterDefaultExceptionMappers(Boolean.FALSE);
    defaultServerFactory.setAdminContextPath("/admin");
    defaultServerFactory.setAdminConnectors(singletonList(getDefaultAdminConnectorFactory()));
    defaultServerFactory.setApplicationConnectors(singletonList(getDefaultApplicationConnectorFactory()));
    defaultServerFactory.setRequestLogFactory(getDefaultLogbackAccessRequestLogFactory());
    defaultServerFactory.setMaxThreads(512);
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

  public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
    SwaggerBundleConfiguration defaultSwaggerBundleConfiguration = new SwaggerBundleConfiguration();
    String resourcePackage = String.join(",", getUniquePackages(getResourceClasses()));
    defaultSwaggerBundleConfiguration.setResourcePackage(resourcePackage);
    defaultSwaggerBundleConfiguration.setSchemes(new String[] {"https", "http"});
    defaultSwaggerBundleConfiguration.setHost("{{localhost}}");
    defaultSwaggerBundleConfiguration.setTitle("PMS API Reference");
    defaultSwaggerBundleConfiguration.setVersion("2.0");
    return Optional.ofNullable(swaggerBundleConfiguration).orElse(defaultSwaggerBundleConfiguration);
  }

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(RESOURCE_PACKAGE, NG_TRIGGER_RESOURCE_PACKAGE, FILTER_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }

  private ConnectorFactory getDefaultApplicationConnectorFactory() {
    final HttpConnectorFactory factory = new HttpConnectorFactory();
    factory.setPort(12001);
    return factory;
  }

  private ConnectorFactory getDefaultAdminConnectorFactory() {
    final HttpConnectorFactory factory = new HttpConnectorFactory();
    factory.setPort(12002);
    return factory;
  }

  private RequestLogFactory getDefaultLogbackAccessRequestLogFactory() {
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

  private static Set<String> getUniquePackages(Collection<Class<?>> classes) {
    return classes.stream().map(aClass -> aClass.getPackage().getName()).collect(toSet());
  }
}
