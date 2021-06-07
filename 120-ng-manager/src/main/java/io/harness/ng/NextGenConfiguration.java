package io.harness.ng;

import static java.util.stream.Collectors.toSet;

import io.harness.AccessControlClientConfiguration;
import io.harness.Microservice;
import io.harness.accesscontrol.AccessControlAdminClientConfiguration;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cf.CfClientConfig;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.ff.FeatureFlagConfig;
import io.harness.file.FileServiceConfiguration;
import io.harness.gitsync.GitSdkConfiguration;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.licensing.LicenseConfiguration;
import io.harness.lock.DistributedLockImplementation;
import io.harness.logstreaming.LogStreamingServiceConfiguration;
import io.harness.mongo.MongoConfig;
import io.harness.ng.core.NextGenConfig;
import io.harness.notification.NotificationClientConfiguration;
import io.harness.outbox.OutboxPollConfiguration;
import io.harness.redis.RedisConfig;
import io.harness.remote.CEAwsSetupConfig;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.resourcegroupclient.remote.ResourceGroupClientConfig;
import io.harness.signup.SignupNotificationConfiguration;
import io.harness.telemetry.segment.SegmentConfiguration;
import io.harness.timescaledb.TimeScaleDBConfig;
import io.harness.yaml.schema.client.config.YamlSchemaClientConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.Path;
import lombok.Getter;
import lombok.Setter;
import org.reflections.Reflections;

@Getter
@OwnedBy(HarnessTeam.PL)
public class NextGenConfiguration extends Configuration {
  public static final String SERVICE_ID = "ng-manager";
  public static final String BASE_PACKAGE = "io.harness.ng";
  public static final String CONNECTOR_PACKAGE = "io.harness.connector.apis.resource";
  public static final String GIT_SYNC_PACKAGE = "io.harness.gitsync";
  public static final String CDNG_RESOURCES_PACKAGE = "io.harness.cdng";
  public static final String OVERLAY_INPUT_SET_RESOURCE_PACKAGE = "io.harness.ngpipeline";
  public static final String YAML_PACKAGE = "io.harness.yaml";
  public static final String FILTER_PACKAGE = "io.harness.filter";
  public static final String SIGNUP_PACKAGE = "io.harness.signup";
  public static final String MOCKSERVER_PACKAGE = "io.harness.ng.core.acl.mockserver";
  public static final String ACCOUNT_PACKAGE = "io.harness.account.resource";
  public static final String LICENSE_PACKAGE = "io.harness.licensing.api.resource";

  @JsonProperty("swagger") private SwaggerBundleConfiguration swaggerBundleConfiguration;
  @Setter @JsonProperty("mongo") private MongoConfig mongoConfig;
  @JsonProperty("pmsMongo") private MongoConfig pmsMongoConfig;
  @JsonProperty("allowedOrigins") private List<String> allowedOrigins = Lists.newArrayList();
  @JsonProperty("managerClientConfig") private ServiceHttpClientConfig managerClientConfig;
  @JsonProperty("grpcClient") private GrpcClientConfig grpcClientConfig;
  @JsonProperty("grpcServer") private GrpcServerConfig grpcServerConfig;
  @JsonProperty("nextGen") private NextGenConfig nextGenConfig;
  @JsonProperty("ciDefaultEntityConfiguration")
  private io.harness.ng.CiDefaultEntityConfiguration ciDefaultEntityConfiguration;
  @JsonProperty("ngManagerClientConfig") private ServiceHttpClientConfig ngManagerClientConfig;
  @JsonProperty("pipelineServiceClientConfig") private ServiceHttpClientConfig pipelineServiceClientConfig;
  @JsonProperty("auditClientConfig") private ServiceHttpClientConfig auditClientConfig;
  @JsonProperty("eventsFramework") private EventsFrameworkConfiguration eventsFrameworkConfiguration;
  @JsonProperty("redisLockConfig") private RedisConfig redisLockConfig;
  @JsonProperty(value = "enableAuth", defaultValue = "true") private boolean enableAuth;
  @JsonProperty("ceAwsSetupConfig") private CEAwsSetupConfig ceAwsSetupConfig;
  @JsonProperty(value = "enableAudit") private boolean enableAudit;
  @JsonProperty(value = "ngAuthUIEnabled") private boolean isNGAuthUIEnabled;
  @JsonProperty("pmsSdkGrpcServerConfig") private GrpcServerConfig pmsSdkGrpcServerConfig;
  @JsonProperty("pmsGrpcClientConfig") private GrpcClientConfig pmsGrpcClientConfig;
  @JsonProperty("shouldConfigureWithPMS") private Boolean shouldConfigureWithPMS;
  @JsonProperty("accessControlClient") private AccessControlClientConfiguration accessControlClientConfiguration;
  @JsonProperty("logStreamingServiceConfig") private LogStreamingServiceConfiguration logStreamingServiceConfig;
  @JsonProperty("gitSyncServerConfig") private GrpcServerConfig gitSyncGrpcServerConfig;
  @JsonProperty("gitGrpcClientConfigs") private Map<Microservice, GrpcClientConfig> gitGrpcClientConfigs;
  @JsonProperty("shouldDeployWithGitSync") private Boolean shouldDeployWithGitSync;
  @JsonProperty("notificationClient") private NotificationClientConfiguration notificationClientConfiguration;
  @JsonProperty("resourceGroupClientConfig") private ResourceGroupClientConfig resourceGroupClientConfig;
  @JsonProperty("accessControlAdminClient")
  private AccessControlAdminClientConfiguration accessControlAdminClientConfiguration;
  @JsonProperty("outboxPollConfig") private OutboxPollConfiguration outboxPollConfig;
  @JsonProperty("segmentConfiguration") private SegmentConfiguration segmentConfiguration;
  @JsonProperty("gitSdkConfiguration") private GitSdkConfiguration gitSdkConfiguration;
  @JsonProperty("fileServiceConfiguration") private FileServiceConfiguration fileServiceConfiguration;
  @JsonProperty("baseUrls") private BaseUrls baseUrls;
  @JsonProperty(value = "enableDefaultResourceGroupCreation", defaultValue = "false")
  private boolean enableDefaultResourceGroupCreation;
  @JsonProperty("cfClientConfig") private CfClientConfig cfClientConfig;
  @JsonProperty("featureFlagConfig") private FeatureFlagConfig featureFlagConfig;
  @JsonProperty("timescaledb") private TimeScaleDBConfig timeScaleDBConfig;
  @JsonProperty("enableDashboardTimescale") private Boolean enableDashboardTimescale;
  @JsonProperty("yamlSchemaClientConfig") private YamlSchemaClientConfig yamlSchemaClientConfig;
  @JsonProperty("distributedLockImplementation") private DistributedLockImplementation distributedLockImplementation;
  @JsonProperty("signupNotificationConfiguration")
  private SignupNotificationConfiguration signupNotificationConfiguration;
  @JsonProperty("useRedisForSdkResponseEvents") private Boolean useRedisForSdkResponseEvents;
  @JsonProperty("licenseConfiguration") private LicenseConfiguration licenseConfiguration;

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
        OVERLAY_INPUT_SET_RESOURCE_PACKAGE, YAML_PACKAGE, FILTER_PACKAGE, SIGNUP_PACKAGE, MOCKSERVER_PACKAGE,
        ACCOUNT_PACKAGE, LICENSE_PACKAGE);
    return reflections.getTypesAnnotatedWith(Path.class);
  }

  private static Set<String> getUniquePackages(Collection<Class<?>> classes) {
    return classes.stream().map(aClass -> aClass.getPackage().getName()).collect(toSet());
  }
}
