package io.harness.ng;

import static java.util.stream.Collectors.toSet;

import io.harness.AccessControlClientConfiguration;
import io.harness.accesscontrol.AccessControlAdminClientConfiguration;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.logstreaming.LogStreamingServiceConfiguration;
import io.harness.mongo.MongoConfig;
import io.harness.ng.accesscontrol.migrations.AccessControlMigrationConfiguration;
import io.harness.ng.core.NextGenConfig;
import io.harness.ng.core.invites.ext.mail.SmtpConfig;
import io.harness.outbox.OutboxEventIteratorConfiguration;
import io.harness.redis.RedisConfig;
import io.harness.remote.CEAwsSetupConfig;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.resourcegroup.ResourceGroupConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.Path;
import lombok.Getter;
import org.reflections.Reflections;

@Getter
public class NextGenConfiguration extends Configuration {
  public static final String SERVICE_ID = "ng-manager";
  public static final String BASE_PACKAGE = "io.harness.ng";
  public static final String RESOURCEGROUP_PACKAGE = "io.harness.resourcegroup";
  public static final String CONNECTOR_PACKAGE = "io.harness.connector.apis.resource";
  public static final String GIT_SYNC_PACKAGE = "io.harness.gitsync";
  public static final String CDNG_RESOURCES_PACKAGE = "io.harness.cdng";
  public static final String OVERLAY_INPUT_SET_RESOURCE_PACKAGE = "io.harness.ngpipeline";
  public static final String YAML_PACKAGE = "io.harness.yaml";
  public static final String FILTER_PACKAGE = "io.harness.filter";
  public static final String SIGNUP_PACKAGE = "io.harness.signup";

  @JsonProperty("swagger") private SwaggerBundleConfiguration swaggerBundleConfiguration;
  @JsonProperty("mongo") private MongoConfig mongoConfig;
  @JsonProperty("pmsMongo") private MongoConfig pmsMongoConfig;
  @JsonProperty("allowedOrigins") private List<String> allowedOrigins = Lists.newArrayList();
  @JsonProperty("managerClientConfig") private ServiceHttpClientConfig managerClientConfig;
  @JsonProperty("grpcClient") private GrpcClientConfig grpcClientConfig;
  @JsonProperty("grpcServer") private GrpcServerConfig grpcServerConfig;
  @JsonProperty("nextGen") private NextGenConfig nextGenConfig;
  @JsonProperty("ciDefaultEntityConfiguration") private CiDefaultEntityConfiguration ciDefaultEntityConfiguration;
  @JsonProperty("ngManagerClientConfig") private ServiceHttpClientConfig ngManagerClientConfig;
  @JsonProperty("eventsFramework") private EventsFrameworkConfiguration eventsFrameworkConfiguration;
  @JsonProperty("redisLockConfig") private RedisConfig redisLockConfig;
  @JsonProperty(value = "enableAuth", defaultValue = "true") private boolean enableAuth;
  @JsonProperty("smtp") private SmtpConfig smtpConfig;
  @JsonProperty("ceAwsSetupConfig") private CEAwsSetupConfig ceAwsSetupConfig;

  @JsonProperty("pmsSdkGrpcServerConfig") private GrpcServerConfig pmsSdkGrpcServerConfig;
  @JsonProperty("pmsGrpcClientConfig") private GrpcClientConfig pmsGrpcClientConfig;
  @JsonProperty("shouldConfigureWithPMS") private Boolean shouldConfigureWithPMS;
  @JsonProperty("ngManagerPublicBaseUrl") private String ngManagerPublicBaseUrl;
  @JsonProperty("baseUrls") private BaseUrls baseUrls;
  @JsonProperty("resourceGroupConfig") private ResourceGroupConfig resoureGroupConfig;
  @JsonProperty("accessControlClient") private AccessControlClientConfiguration accessControlClientConfiguration;
  @JsonProperty("logStreamingServiceConfig") private LogStreamingServiceConfiguration logStreamingServiceConfig;
  @JsonProperty("gitSyncServerConfig") private GrpcServerConfig gitSyncGrpcServerConfig;
  @JsonProperty("shouldDeployWithGitSync") private Boolean shouldDeployWithGitSync;
  @JsonProperty("accessControlAdminClient")
  private AccessControlAdminClientConfiguration accessControlAdminClientConfiguration;
  @JsonProperty("outboxIteratorConfig") private OutboxEventIteratorConfiguration outboxIteratorConfig;
  @JsonProperty("accessControlMigrationConfig")
  private AccessControlMigrationConfiguration accessControlMigrationConfig;

  // [secondary-db]: Uncomment this and the corresponding config in yaml file if you want to connect to another database
  //  @JsonProperty("secondary-mongo") MongoConfig secondaryMongoConfig;

  public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
    SwaggerBundleConfiguration defaultSwaggerBundleConfiguration = new SwaggerBundleConfiguration();

    String resourcePackage = String.join(",", getUniquePackages(getResourceClasses()));
    defaultSwaggerBundleConfiguration.setResourcePackage(resourcePackage);
    defaultSwaggerBundleConfiguration.setSchemes(new String[] {"https", "http"});
    defaultSwaggerBundleConfiguration.setHost(
        "localhost"); // TODO, we should set the appropriate host here ex: qa.harness.io etc
    defaultSwaggerBundleConfiguration.setTitle("CD NextGen API Reference");
    defaultSwaggerBundleConfiguration.setVersion("2.0");

    return Optional.ofNullable(swaggerBundleConfiguration).orElse(defaultSwaggerBundleConfiguration);
  }

  public static Collection<Class<?>> getResourceClasses() {
    Reflections reflections = new Reflections(BASE_PACKAGE, CONNECTOR_PACKAGE, GIT_SYNC_PACKAGE, CDNG_RESOURCES_PACKAGE,
        OVERLAY_INPUT_SET_RESOURCE_PACKAGE, YAML_PACKAGE, FILTER_PACKAGE, RESOURCEGROUP_PACKAGE, SIGNUP_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }

  private static Set<String> getUniquePackages(Collection<Class<?>> classes) {
    return classes.stream().map(aClass -> aClass.getPackage().getName()).collect(toSet());
  }
}
