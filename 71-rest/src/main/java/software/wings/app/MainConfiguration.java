package software.wings.app;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.singletonList;

import com.google.common.collect.ImmutableList;
import com.google.inject.Singleton;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import io.harness.cache.CacheConfig;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.commandlibrary.CommandLibraryServiceConfig;
import io.harness.config.DatadogConfig;
import io.harness.config.MockServerConfig;
import io.harness.config.PipelineConfig;
import io.harness.config.PublisherConfiguration;
import io.harness.config.WorkersConfiguration;
import io.harness.configuration.DeployMode;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.event.handler.segment.SalesforceConfig;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.lock.DistributedLockImplementation;
import io.harness.logstreaming.LogStreamingServiceConfig;
import io.harness.mongo.MongoConfig;
import io.harness.organizationmanagerclient.OrganizationManagerClientConfig;
import io.harness.redis.RedisConfig;
import io.harness.scheduler.SchedulerConfig;
import io.harness.stream.AtmosphereBroadcaster;
import io.harness.timescaledb.TimeScaleDBConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.DataStorageMode;
import software.wings.audit.AuditConfig;
import software.wings.beans.DefaultSalesContacts;
import software.wings.beans.HttpMethod;
import software.wings.beans.UrlInfo;
import software.wings.beans.security.access.GlobalWhitelistConfig;
import software.wings.cdn.CdnConfig;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.jre.JreConfig;
import software.wings.search.framework.ElasticsearchConfig;
import software.wings.security.authentication.MarketPlaceConfig;
import software.wings.security.authentication.oauth.AzureConfig;
import software.wings.security.authentication.oauth.BitbucketConfig;
import software.wings.security.authentication.oauth.GithubConfig;
import software.wings.security.authentication.oauth.GitlabConfig;
import software.wings.security.authentication.oauth.GoogleConfig;
import software.wings.security.authentication.oauth.LinkedinConfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Used to load all the application configuration.
 *
 * @author Rishi
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
public class MainConfiguration extends Configuration implements AssetsBundleConfiguration {
  @JsonProperty
  private AssetsConfiguration assetsConfiguration =
      AssetsConfiguration.builder()
          .mimeTypes(of("js", "application/json; charset=UTF-8", "zip", "application/zip"))
          .build();

  @JsonProperty("swagger") private SwaggerBundleConfiguration swaggerBundleConfiguration;
  @JsonProperty("mongo") private MongoConfig mongoConnectionFactory = MongoConfig.builder().build();
  @JsonProperty("distributedLockImplementation") private DistributedLockImplementation distributedLockImplementation;
  @JsonProperty("events-mongo") private MongoConfig eventsMongo = MongoConfig.builder().uri("").build();
  @JsonProperty("elasticsearch")
  private ElasticsearchConfig elasticsearchConfig = ElasticsearchConfig.builder().build();
  @JsonProperty(value = "searchEnabled") private boolean isSearchEnabled;
  @JsonProperty private PortalConfig portal = new PortalConfig();
  @JsonProperty(defaultValue = "true") private boolean enableIterators = true;
  @JsonProperty(defaultValue = "true") private boolean enableAuth = true;
  @JsonProperty(defaultValue = "50") private int jenkinsBuildQuerySize = 50;
  @JsonProperty private FileUploadLimit fileUploadLimits = new FileUploadLimit();
  @JsonProperty("backgroundScheduler") private SchedulerConfig backgroundSchedulerConfig = new SchedulerConfig();
  @JsonProperty("serviceScheduler") private SchedulerConfig serviceSchedulerConfig = new SchedulerConfig();
  @JsonProperty("watcherMetadataUrl") private String watcherMetadataUrl;
  @JsonProperty("delegateMetadataUrl") private String delegateMetadataUrl;
  @JsonProperty("awsInstanceTypes") private List<String> awsInstanceTypes;
  @JsonProperty("awsRegionIdToName") private Map<String, String> awsRegionIdToName;
  @JsonProperty("apiUrl") private String apiUrl;
  @JsonProperty("supportEmail") private String supportEmail;
  @JsonProperty("envPath") private String envPath;
  @JsonProperty("smtp") private SmtpConfig smtpConfig;
  @JsonProperty("globalWhitelistConfig") private GlobalWhitelistConfig globalWhitelistConfig;
  @JsonProperty(defaultValue = "KUBERNETES") private DeployMode deployMode = DeployMode.KUBERNETES;
  @JsonProperty("featuresEnabled") private String featureNames;
  @JsonProperty("kubectlVersion") private String kubectlVersion;
  @JsonProperty("ocVersion") private String ocVersion;
  @JsonProperty("trialRegistrationAllowed") private boolean trialRegistrationAllowed;
  @JsonProperty("ngManagerAvailable") private boolean ngManagerAvailable;
  @JsonProperty(value = "trialRegistrationAllowedForBugathon", defaultValue = "false")
  private boolean trialRegistrationAllowedForBugathon;
  @JsonProperty("blacklistedEmailDomainsAllowed") private boolean blacklistedEmailDomainsAllowed;
  @JsonProperty("pwnedPasswordsAllowed") private boolean pwnedPasswordsAllowed;
  @JsonProperty("auditConfig") private AuditConfig auditConfig;
  @JsonProperty("executionLogStorageMode") private DataStorageMode executionLogsStorageMode;
  @JsonProperty("fileStorageMode") private DataStorageMode fileStorageMode;
  @JsonProperty("clusterName") private String clusterName;
  @JsonProperty("ceSetUpConfig") private CESetUpConfig ceSetUpConfig;
  @JsonProperty("marketoConfig") private MarketoConfig marketoConfig;
  @JsonProperty("segmentConfig") private SegmentConfig segmentConfig;
  @JsonProperty("salesforceConfig") private SalesforceConfig salesforceConfig = SalesforceConfig.builder().build();
  @JsonProperty("datadogConfig") private DatadogConfig datadogConfig;
  @JsonProperty("redisLockConfig") private RedisConfig redisLockConfig;
  @JsonProperty("redisAtmosphereConfig") private RedisConfig redisAtmosphereConfig;
  @JsonProperty("defaultSalesContacts") private DefaultSalesContacts defaultSalesContacts;
  @JsonProperty("githubConfig") private GithubConfig githubConfig;
  @JsonProperty("linkedinConfig") private LinkedinConfig linkedinConfig;
  @JsonProperty("googleConfig") private GoogleConfig googleConfig;
  @JsonProperty("azureConfig") private AzureConfig azureConfig;
  @JsonProperty("bitbucketConfig") private BitbucketConfig bitbucketConfig;
  @JsonProperty("gitlabConfig") private GitlabConfig gitlabConfig;
  @JsonProperty("mktPlaceConfig") private MarketPlaceConfig marketPlaceConfig;
  @JsonProperty("sampleTargetEnv") private String sampleTargetEnv;
  @JsonProperty("sampleTargetStatusHost") private String sampleTargetStatusHost;
  @JsonProperty("timescaledb") private TimeScaleDBConfig timeScaleDBConfig;
  @JsonProperty("cacheConfig") private CacheConfig cacheConfig;
  @JsonProperty("techStacks") private Map<String, UrlInfo> techStackLinks;
  @JsonProperty("grpcServerConfig") private GrpcServerConfig grpcServerConfig;
  @JsonProperty("grpcDelegateServiceClientConfig") private GrpcClientConfig grpcDelegateServiceClientConfig;
  @JsonProperty("grpcClientConfig") private GrpcClientConfig grpcClientConfig;
  @JsonProperty("workers") private WorkersConfiguration workers;
  @JsonProperty("publishers") private PublisherConfiguration publisherConfiguration;
  @JsonProperty("pipelineConfig") private PipelineConfig pipelineConfig = new PipelineConfig();
  @JsonProperty("currentJre") private String currentJre;
  @JsonProperty("migrateToJre") private String migrateToJre;
  @JsonProperty("jreConfigs") private Map<String, JreConfig> jreConfigs;
  @JsonProperty("cdnConfig") private CdnConfig cdnConfig;
  @JsonProperty("commandLibraryServiceConfig")
  private CommandLibraryServiceConfig commandLibraryServiceConfig = CommandLibraryServiceConfig.builder().build();
  @JsonProperty(value = "bugsnagApiKey") private String bugsnagApiKey;
  @JsonProperty("atmosphereBroadcaster") private AtmosphereBroadcaster atmosphereBroadcaster;
  @JsonProperty(value = "jobsFrequencyConfig") private JobsFrequencyConfig jobsFrequencyConfig;
  @JsonProperty("organizationManagerClient") private OrganizationManagerClientConfig organizationManagerClientConfig;
  @JsonProperty("mockServerConfig") private MockServerConfig mockServerConfig;
  @JsonProperty("numberOfRemindersBeforeAccountDeletion") private int numberOfRemindersBeforeAccountDeletion;
  @JsonProperty("delegateGrpcServicePort") private Integer delegateGrpcServicePort;
  @JsonProperty("logStreamingServiceConfig") private LogStreamingServiceConfig logStreamingServiceConfig;

  private int applicationPort;
  private boolean sslEnabled;

  private static final String IS_OPTION_HEAD_HTTP_METHOD_BLOCKED = "IS_OPTION_HEAD_REQUEST_METHOD_BLOCKED";

  /**
   * Instantiates a new Main configuration.
   */
  public MainConfiguration() {
    DefaultServerFactory defaultServerFactory = new DefaultServerFactory();
    defaultServerFactory.setJerseyRootPath("/api");
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
   * Gets swagger bundle configuration.
   *
   * @return the swagger bundle configuration
   */
  public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
    SwaggerBundleConfiguration defaultSwaggerBundleConfiguration = new SwaggerBundleConfiguration();
    defaultSwaggerBundleConfiguration.setResourcePackage(
        "software.wings.resources,software.wings.utils,io.harness.cvng.core.resources");
    defaultSwaggerBundleConfiguration.setSchemes(new String[] {"https", "http"});
    defaultSwaggerBundleConfiguration.setHost("{{host}}");
    return Optional.ofNullable(swaggerBundleConfiguration).orElse(defaultSwaggerBundleConfiguration);
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

  /**
   * Created by peeyushaggarwal on 8/30/16.
   */
  public abstract static class AssetsConfigurationMixin {
    /**
     * Gets resource path to uri mappings.
     *
     * @return the resource path to uri mappings
     */
    @JsonIgnore public abstract Map<String, String> getResourcePathToUriMappings();
  }
}
