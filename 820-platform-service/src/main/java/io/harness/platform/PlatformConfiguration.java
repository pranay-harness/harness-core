package io.harness.platform;

import io.harness.grpc.client.GrpcClientConfig;
import io.harness.mongo.MongoConfig;
import io.harness.notification.SeedDataConfiguration;
import io.harness.notification.SmtpConfig;
import io.harness.remote.client.ServiceHttpClientConfig;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.dropwizard.Configuration;
import io.dropwizard.logging.FileAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.request.logging.RequestLogFactory;
import io.dropwizard.server.DefaultServerFactory;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.Path;
import lombok.Getter;
import org.reflections.Reflections;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlatformConfiguration extends Configuration {
  public static final String SERVICE_ID = "platform-microservice";
  public static final String BASE_PACKAGE = "io.harness.platform";
  public static final String PLATFORM_RESOURCE_PACKAGE = "io.harness.platform.remote";
  public static final String NOTIFICATION_RESOURCE_PACKAGE = "io.harness.notification.remote.resources";
  @JsonProperty("mongo") private MongoConfig mongoConfig;
  @JsonProperty("allowedOrigins") private List<String> allowedOrigins = Lists.newArrayList();
  @JsonProperty("managerClientConfig") private ServiceHttpClientConfig serviceHttpClientConfig;
  @JsonProperty("rbacServiceConfig") private ServiceHttpClientConfig rbacServiceConfig;
  @JsonProperty("secrets") private PlatformSecrets platformSecrets;
  @JsonProperty(value = "enableAuth", defaultValue = "true") private boolean enableAuth;
  @JsonProperty("smtp") private SmtpConfig smtpConfig;
  @JsonProperty(value = "environment", defaultValue = "dev") private String environment;
  @JsonProperty("seedDataConfiguration") private SeedDataConfiguration seedDataConfiguration;
  @JsonProperty("grpcClient") private GrpcClientConfig grpcClientConfig;

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(PLATFORM_RESOURCE_PACKAGE, NOTIFICATION_RESOURCE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }

  public PlatformConfiguration() {
    DefaultServerFactory defaultServerFactory = new DefaultServerFactory();
    defaultServerFactory.setJerseyRootPath("/api");
    defaultServerFactory.setRequestLogFactory(getDefaultlogbackAccessRequestLogFactory());
    super.setServerFactory(defaultServerFactory);
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
