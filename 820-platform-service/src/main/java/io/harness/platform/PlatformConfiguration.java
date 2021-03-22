package io.harness.platform;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.platform.audit.AuditServiceConfiguration;
import io.harness.platform.notification.NotificationServiceConfiguration;
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
@OwnedBy(PL)
public class PlatformConfiguration extends Configuration {
  public static final String SERVICE_ID = "platform-microservice";
  public static final String BASE_PACKAGE = "io.harness.platform";
  public static final String PLATFORM_RESOURCE_PACKAGE = "io.harness.platform.remote";
  public static final String NOTIFICATION_RESOURCE_PACKAGE = "io.harness.notification.remote.resources";
  public static final String AUDIT_RESOURCE_PACKAGE = "io.harness.audit.remote";
  public static final String FILTER_RESOURCE_PACKAGE = "io.harness.filter";

  @JsonProperty("notificationServiceConfig") private NotificationServiceConfiguration notificationServiceConfig;
  @JsonProperty("auditServiceConfig") private AuditServiceConfiguration auditServiceConfig;

  @JsonProperty("allowedOrigins") private List<String> allowedOrigins = Lists.newArrayList();
  @JsonProperty("managerClientConfig") private ServiceHttpClientConfig serviceHttpClientConfig;
  @JsonProperty("rbacServiceConfig") private ServiceHttpClientConfig rbacServiceConfig;
  @JsonProperty("secrets") private PlatformSecrets platformSecrets;
  @JsonProperty(value = "enableAuth", defaultValue = "true") private boolean enableAuth;
  @JsonProperty(value = "environment", defaultValue = "dev") private String environment;

  public static Collection<Class<?>> getNotificationServiceResourceClasses() {
    Reflections reflections = new Reflections(NOTIFICATION_RESOURCE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }

  public static Collection<Class<?>> getAuditServiceResourceClasses() {
    Reflections reflections = new Reflections(AUDIT_RESOURCE_PACKAGE, FILTER_RESOURCE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }

  public static Collection<Class<?>> getPlatformServiceCombinedResourceClasses() {
    Collection<Class<?>> resources = getNotificationServiceResourceClasses();
    resources.addAll(getAuditServiceResourceClasses());
    return resources;
  }

  public PlatformConfiguration() {
    DefaultServerFactory defaultServerFactory = new DefaultServerFactory();
    defaultServerFactory.setJerseyRootPath("/api");
    defaultServerFactory.setRequestLogFactory(getDefaultlogbackAccessRequestLogFactory());
    super.setServerFactory(defaultServerFactory);
  }

  private RequestLogFactory<?> getDefaultlogbackAccessRequestLogFactory() {
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
