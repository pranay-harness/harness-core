package io.harness.ccm;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static java.util.stream.Collectors.toSet;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.beans.config.AwsConfig;
import io.harness.ccm.commons.beans.config.GcpConfig;
import io.harness.cf.CfClientConfig;
import io.harness.configuration.DeployMode;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.ff.FeatureFlagConfig;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.mongo.MongoConfig;
import io.harness.remote.CEAzureSetupConfig;
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
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.Path;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;

@Getter
@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(CE)
public class CENextGenConfiguration extends Configuration {
  public static final String SERVICE_ROOT_PATH = "/ccm/api";
  public static final String SERVICE_ID = "cenextgen-microservice";
  public static final String BASE_PACKAGE = "io.harness.ccm";

  public static final List<String> RESOURCE_PACKAGES = ImmutableList.of("io.harness.ccm.remote.resources");

  @JsonProperty("swagger") private SwaggerBundleConfiguration swaggerBundleConfiguration;
  @Setter @JsonProperty("events-mongo") private MongoConfig eventsMongoConfig;
  @JsonProperty("allowedOrigins") private List<String> allowedOrigins = Lists.newArrayList();
  @JsonProperty("managerClientConfig") private ServiceHttpClientConfig managerClientConfig;
  @JsonProperty("ngManagerClientConfig") private ServiceHttpClientConfig ngManagerClientConfig;
  @JsonProperty("grpcClient") private GrpcClientConfig grpcClientConfig;
  @JsonProperty(value = "enableAuth", defaultValue = "false") private boolean enableAuth;
  @JsonProperty("ngManagerServiceSecret") private String ngManagerServiceSecret;
  @JsonProperty("jwtAuthSecret") private String jwtAuthSecret;
  @JsonProperty("jwtIdentityServiceSecret") private String jwtIdentityServiceSecret;

  @JsonProperty(defaultValue = "KUBERNETES") private DeployMode deployMode = DeployMode.KUBERNETES;
  @JsonProperty(value = "featureFlagsEnabled", defaultValue = "") private String featureFlagsEnabled;
  @JsonProperty("cfClientConfig") private CfClientConfig cfClientConfig;
  @JsonProperty("featureFlagConfig") private FeatureFlagConfig featureFlagConfig;
  @JsonProperty("eventsFramework") private EventsFrameworkConfiguration eventsFrameworkConfiguration;
  @JsonProperty("timescaledb") private TimeScaleDBConfig timeScaleDBConfig;

  @JsonProperty(value = "gcpConfig") private GcpConfig gcpConfig;
  @JsonProperty(value = "ceAzureSetupConfig") private CEAzureSetupConfig ceAzureSetupConfig;
  @JsonProperty(value = "awsConfig") private AwsConfig awsConfig;

  @JsonProperty(value = "hostname") private String hostname;
  @JsonProperty(value = "basePathPrefix") private String basePathPrefix;

  public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
    SwaggerBundleConfiguration defaultSwaggerConf = new SwaggerBundleConfiguration();

    String resourcePackage = String.join(",", getUniquePackages(getResourceClasses()));
    defaultSwaggerConf.setResourcePackage(resourcePackage);
    defaultSwaggerConf.setSchemes(new String[] {"https", "http"});
    defaultSwaggerConf.setTitle("CE NextGen API Reference");
    defaultSwaggerConf.setVersion("1.0");

    return Optional.ofNullable(swaggerBundleConfiguration).orElse(defaultSwaggerConf);
  }

  private static Set<String> getUniquePackages(Collection<Class<?>> classes) {
    return classes.stream().map(aClass -> aClass.getPackage().getName()).collect(toSet());
  }

  public static Collection<Class<?>> getResourceClasses() {
    final Reflections reflections = new Reflections(RESOURCE_PACKAGES);

    return reflections.getTypesAnnotatedWith(Path.class);
  }

  public CENextGenConfiguration() {
    DefaultServerFactory defaultServerFactory = new DefaultServerFactory();
    defaultServerFactory.setJerseyRootPath(SERVICE_ROOT_PATH);
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

  protected OpenAPIConfiguration getOasConfig() {
    OpenAPI oas = new OpenAPI();
    Info info =
        new Info()
            .title("CCM NextGen API Reference")
            .description(
                "This is the Open Api Spec 3 for the CCM NextGen Manager. This is under active development. Beware of the breaking change with respect to the generated code stub")
            .termsOfService("https://harness.io/terms-of-use/")
            .version("3.0")
            .contact(new Contact().email("contact@harness.io"));
    oas.info(info);

    List<Server> serversList = new ArrayList<>();
    serversList.add(new Server().url(SERVICE_ROOT_PATH));

    try {
      URL baseurl = new URL("https", hostname, basePathPrefix);
      serversList.add(new Server().url(baseurl.toString()));
    } catch (MalformedURLException e) {
      log.error("failed to set baseurl for server, {}/{}", hostname, basePathPrefix);
    }

    oas.servers(serversList);

    final Set<String> packages = getUniquePackages(getResourceClasses());

    return new SwaggerConfiguration().openAPI(oas).prettyPrint(true).resourcePackages(packages).scannerClass(
        "io.swagger.v3.jaxrs2.integration.JaxrsAnnotationScanner");
  }
}
