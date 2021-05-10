package io.harness.ccm;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cf.CfClientConfig;
import io.harness.cf.CfMigrationConfig;
import io.harness.configuration.DeployMode;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.mongo.MongoConfig;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.timescaledb.TimeScaleDBConfig;

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
@OwnedBy(CE)
public class CENextGenConfiguration extends Configuration {
  public static final String SERVICE_ID = "cenextgen-microservice";
  public static final String BASE_PACKAGE = "io.harness.ccm";
  public static final String RESOURCE_PACKAGE = "io.harness.ccm.remote.resources";

  @JsonProperty("events-mongo") private MongoConfig eventsMongoConfig;
  @JsonProperty("allowedOrigins") private List<String> allowedOrigins = Lists.newArrayList();
  @JsonProperty("managerClientConfig") private ServiceHttpClientConfig managerClientConfig;
  @JsonProperty("ngManagerClientConfig") private ServiceHttpClientConfig ngManagerClientConfig;
  @JsonProperty(value = "enableAuth", defaultValue = "false") private boolean enableAuth;
  @JsonProperty("ngManagerServiceSecret") private String ngManagerServiceSecret;
  @JsonProperty("jwtAuthSecret") private String jwtAuthSecret;
  @JsonProperty("jwtIdentityServiceSecret") private String jwtIdentityServiceSecret;

  @JsonProperty(defaultValue = "KUBERNETES") private DeployMode deployMode = DeployMode.KUBERNETES;
  @JsonProperty(value = "featureFlagsEnabled", defaultValue = "") private String featureFlagsEnabled;
  @JsonProperty("cfClientConfig") private CfClientConfig cfClientConfig;
  @JsonProperty("cfMigrationConfig") private CfMigrationConfig cfMigrationConfig;
  @JsonProperty("eventsFramework") private EventsFrameworkConfiguration eventsFrameworkConfiguration;
  @JsonProperty("timescaledb") private TimeScaleDBConfig timeScaleDBConfig;

  @JsonProperty(value = "awsConnectorTemplate", defaultValue = "") private String awsConnectorTemplate;

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(RESOURCE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }

  public CENextGenConfiguration() {
    DefaultServerFactory defaultServerFactory = new DefaultServerFactory();
    defaultServerFactory.setJerseyRootPath("/ccm/api");
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
