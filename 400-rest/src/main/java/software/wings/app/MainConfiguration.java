package software.wings.app;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.singletonList;

import io.harness.AccessControlClientConfiguration;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.cache.CacheConfig;
import io.harness.ccm.setup.config.CESetUpConfig;
import io.harness.cf.CfClientConfig;
import io.harness.cf.CfMigrationConfig;
import io.harness.commandlibrary.CommandLibraryServiceConfig;
import io.harness.config.DatadogConfig;
import io.harness.config.GcpMarketplaceConfig;
import io.harness.config.MockServerConfig;
import io.harness.config.PipelineConfig;
import io.harness.config.PublisherConfiguration;
import io.harness.config.WorkersConfiguration;
import io.harness.configuration.DeployMode;
import io.harness.configuration.DeployVersion;
import io.harness.delegate.beans.FileUploadLimit;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.event.handler.segment.SalesforceConfig;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.eventsframework.EventsFrameworkConfiguration;
import io.harness.ff.FeatureFlagConfig;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.grpc.server.GrpcServerConfig;
import io.harness.lock.DistributedLockImplementation;
import io.harness.logstreaming.LogStreamingServiceConfig;
import io.harness.mongo.MongoConfig;
import io.harness.redis.RedisConfig;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.scheduler.SchedulerConfig;
import io.harness.secret.ConfigSecret;
import io.harness.stream.AtmosphereBroadcaster;
import io.harness.timescaledb.TimeScaleDBConfig;

import software.wings.DataStorageMode;
import software.wings.audit.AuditConfig;
import software.wings.beans.DefaultSalesContacts;
import software.wings.beans.HttpMethod;
import software.wings.beans.UrlInfo;
import software.wings.beans.security.access.GlobalWhitelistConfig;
import software.wings.cdn.CdnConfig;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.jre.JreConfig;
import software.wings.scheduler.LdapSyncJobConfig;
import software.wings.search.framework.ElasticsearchConfig;
import software.wings.security.authentication.MarketPlaceConfig;
import software.wings.security.authentication.oauth.AzureConfig;
import software.wings.security.authentication.oauth.BitbucketConfig;
import software.wings.security.authentication.oauth.GithubConfig;
import software.wings.security.authentication.oauth.GitlabConfig;
import software.wings.security.authentication.oauth.GoogleConfig;
import software.wings.security.authentication.oauth.LinkedinConfig;

import ch.qos.logback.access.spi.IAccessEvent;
import ch.qos.logback.classic.Level;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Used to load all the application configuration.
 */
@TargetModule(HarnessModule._957_CG_BEANS)
@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
@OwnedBy(PL)
public class MainConfiguration extends Configuration implements AssetsBundleConfiguration {
  @JsonProperty
  private AssetsConfiguration assetsConfiguration =
      AssetsConfiguration.builder()
          .mimeTypes(of("js", "application/json; charset=UTF-8", "zip", "application/zip"))
          .build();

  @JsonProperty("swagger") private SwaggerBundleConfiguration swaggerBundleConfiguration;
  @ConfigSecret @JsonProperty("mongo") private MongoConfig mongoConnectionFactory = MongoConfig.builder().build();
  @JsonProperty("distributedLockImplementation") private DistributedLockImplementation distributedLockImplementation;
  @ConfigSecret @JsonProperty("events-mongo") private MongoConfig eventsMongo = MongoConfig.builder().uri("").build();
  @JsonProperty("elasticsearch")
  private ElasticsearchConfig elasticsearchConfig = ElasticsearchConfig.builder().build();
  @JsonProperty(value = "searchEnabled") private boolean isSearchEnabled;
  @ConfigSecret @JsonProperty private PortalConfig portal = new PortalConfig();
  @JsonProperty(defaultValue = "true") private boolean enableIterators = true;
  @JsonProperty(defaultValue = "true") private boolean enableAuth = true;
  @JsonProperty(defaultValue = "50") private int jenkinsBuildQuerySize = 50;
  @JsonProperty("iteratorsConfig") private IteratorsConfig iteratorsConfig;
  @JsonProperty private io.harness.delegate.beans.FileUploadLimit fileUploadLimits = new FileUploadLimit();
  @JsonProperty("backgroundScheduler") private SchedulerConfig backgroundSchedulerConfig = new SchedulerConfig();
  @JsonProperty("serviceScheduler") private SchedulerConfig serviceSchedulerConfig = new SchedulerConfig();
  @JsonProperty("watcherMetadataUrl") private String watcherMetadataUrl;
  @JsonProperty("delegateMetadataUrl") private String delegateMetadataUrl;
  @JsonProperty("awsInstanceTypes") private List<String> awsInstanceTypes;
  @JsonProperty("awsRegionIdToName") private Map<String, String> awsRegionIdToName;
  @JsonProperty("apiUrl") private String apiUrl;
  @JsonProperty("exportAccountDataBatchSize") private int exportAccountDataBatchSize;
  @JsonProperty("supportEmail") private String supportEmail;
  @JsonProperty("envPath") private String envPath;
  @ConfigSecret @JsonProperty("smtp") private SmtpConfig smtpConfig;
  @JsonProperty("globalWhitelistConfig") private GlobalWhitelistConfig globalWhitelistConfig;
  @JsonProperty(defaultValue = "KUBERNETES") private DeployMode deployMode = DeployMode.KUBERNETES;
  @JsonProperty("deployVersion") private DeployVersion deployVersion;
  @JsonProperty("featuresEnabled") private String featureNames;
  @JsonProperty("kubectlVersion") private String kubectlVersion;
  @JsonProperty("ocVersion") private String ocVersion;
  @JsonProperty("scmVersion") private String scmVersion;
  @JsonProperty("trialRegistrationAllowed") private boolean trialRegistrationAllowed;
  @JsonProperty("eventsFrameworkAvailableInOnPrem") private boolean eventsFrameworkAvailableInOnPrem;
  @JsonProperty(value = "trialRegistrationAllowedForBugathon", defaultValue = "false")
  private boolean trialRegistrationAllowedForBugathon;
  @JsonProperty("blacklistedEmailDomainsAllowed") private boolean blacklistedEmailDomainsAllowed;
  @JsonProperty("pwnedPasswordsAllowed") private boolean pwnedPasswordsAllowed;
  @JsonProperty("auditConfig") private AuditConfig auditConfig;
  @JsonProperty("executionLogStorageMode") private DataStorageMode executionLogsStorageMode;
  @JsonProperty("fileStorageMode") private DataStorageMode fileStorageMode;
  @JsonProperty("clusterName") private String clusterName;
  @JsonProperty("deploymentClusterName") private String deploymentClusterName;
  @ConfigSecret @JsonProperty("ceSetUpConfig") private CESetUpConfig ceSetUpConfig;
  @ConfigSecret @JsonProperty("marketoConfig") private MarketoConfig marketoConfig;
  @JsonProperty("segmentConfig") private SegmentConfig segmentConfig;
  @ConfigSecret
  @JsonProperty("salesforceConfig")
  private SalesforceConfig salesforceConfig = SalesforceConfig.builder().build();
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
  @ConfigSecret @JsonProperty("mktPlaceConfig") private MarketPlaceConfig marketPlaceConfig;
  @JsonProperty("sampleTargetEnv") private String sampleTargetEnv;
  @JsonProperty("sampleTargetStatusHost") private String sampleTargetStatusHost;
  @ConfigSecret @JsonProperty("timescaledb") private TimeScaleDBConfig timeScaleDBConfig;
  @JsonProperty("cacheConfig") private CacheConfig cacheConfig;
  @JsonProperty("ngAuthUIEnabled") private boolean ngAuthUIEnabled;
  @JsonProperty("gcpMarketplaceConfig") private GcpMarketplaceConfig gcpMarketplaceConfig;
  @JsonProperty("techStacks") private Map<String, UrlInfo> techStackLinks;
  @JsonProperty("grpcServerConfig") private GrpcServerConfig grpcServerConfig;
  @JsonProperty("grpcDelegateServiceClientConfig") private GrpcClientConfig grpcDelegateServiceClientConfig;
  @JsonProperty("grpcOnpremDelegateClientConfig") private GrpcClientConfig grpcOnpremDelegateClientConfig;
  @JsonProperty("grpcClientConfig") private GrpcClientConfig grpcClientConfig;
  @JsonProperty("grpcDMSClientConfig") private GrpcClientConfig grpcDMSClientConfig;
  @JsonProperty("workers") private WorkersConfiguration workers;
  @JsonProperty("publishers") private PublisherConfiguration publisherConfiguration;
  @JsonProperty("pipelineConfig") private PipelineConfig pipelineConfig = new PipelineConfig();
  @JsonProperty("currentJre") private String currentJre;
  @JsonProperty("migrateToJre") private String migrateToJre;
  @JsonProperty("jreConfigs") private Map<String, JreConfig> jreConfigs;
  @ConfigSecret @JsonProperty("cdnConfig") private CdnConfig cdnConfig;
  @ConfigSecret
  @JsonProperty("commandLibraryServiceConfig")
  private CommandLibraryServiceConfig commandLibraryServiceConfig = CommandLibraryServiceConfig.builder().build();
  @JsonProperty(value = "bugsnagApiKey") private String bugsnagApiKey;
  @JsonProperty("atmosphereBroadcaster") private AtmosphereBroadcaster atmosphereBroadcaster;
  @JsonProperty(value = "jobsFrequencyConfig") private JobsFrequencyConfig jobsFrequencyConfig;
  @JsonProperty("ngManagerServiceHttpClientConfig") private ServiceHttpClientConfig ngManagerServiceHttpClientConfig;
  @JsonProperty("mockServerConfig") private MockServerConfig mockServerConfig;
  @JsonProperty("numberOfRemindersBeforeAccountDeletion") private int numberOfRemindersBeforeAccountDeletion;
  @JsonProperty("delegateGrpcServicePort") private Integer delegateGrpcServicePort;
  @ConfigSecret @JsonProperty("logStreamingServiceConfig") private LogStreamingServiceConfig logStreamingServiceConfig;
  @ConfigSecret
  @JsonProperty("accessControlClient")
  private AccessControlClientConfiguration accessControlClientConfiguration;
  @ConfigSecret @JsonProperty("eventsFramework") private EventsFrameworkConfiguration eventsFrameworkConfiguration;
  @ConfigSecret @JsonProperty("cfClientConfig") private CfClientConfig cfClientConfig;
  @ConfigSecret @JsonProperty("cfMigrationConfig") private CfMigrationConfig cfMigrationConfig;
  @JsonProperty("featureFlagConfig") private FeatureFlagConfig featureFlagConfig;
  @JsonProperty("auditClientConfig") private ServiceHttpClientConfig auditClientConfig;
  @JsonProperty(value = "enableAudit") private boolean enableAudit;
  @ConfigSecret @JsonProperty("dmsSecret") private String dmsSecret;
  @JsonProperty(value = "disableDelegateMgmtInManager", defaultValue = "false")
  private boolean disableDelegateMgmtInManager;
  @JsonProperty(value = "pullSecrets", defaultValue = "false") private boolean pullSecrets;
  @JsonProperty(value = "smProject") private String smProject;
  @ConfigSecret @JsonProperty(value = "captchaSecret") private String captchaSecret;
  @ConfigSecret @JsonProperty(value = "verificationServiceSecretKey") private String verificationServiceSecretKey;
  @JsonProperty("ldapSyncJobConfig") private LdapSyncJobConfig ldapSyncJobConfig;
  @JsonProperty("eventListenersCountConfig") private EventListenersCountConfig eventListenersCountConfig;
  @JsonProperty(value = "useGlobalKMSAsBaseAlgo", defaultValue = "false") private boolean useGlobalKMSAsBaseAlgo;

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
        "software.wings.resources,software.wings.utils,io.harness.cvng.core.resources,io.harness.delegate.resources");
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

  protected ConnectorFactory getDefaultAdminConnectorFactory() {
    final HttpConnectorFactory factory = new HttpConnectorFactory();
    factory.setPort(9091);
    return factory;
  }

  protected ConnectorFactory getDefaultApplicationConnectorFactory() {
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
