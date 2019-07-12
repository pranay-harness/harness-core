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
import io.harness.event.grpc.GrpcServerConfig;
import io.harness.event.handler.marketo.MarketoConfig;
import io.harness.event.handler.segment.SegmentConfig;
import io.harness.mongo.MongoConfig;
import io.harness.scheduler.SchedulerConfig;
import io.harness.timescaledb.TimeScaleDBConfig;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.DataStorageMode;
import software.wings.beans.DefaultSalesContacts;
import software.wings.beans.UrlInfo;
import software.wings.beans.security.access.GlobalWhitelistConfig;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.security.authentication.MarketPlaceConfig;
import software.wings.security.authentication.oauth.AzureConfig;
import software.wings.security.authentication.oauth.BitbucketConfig;
import software.wings.security.authentication.oauth.GithubConfig;
import software.wings.security.authentication.oauth.GitlabConfig;
import software.wings.security.authentication.oauth.GoogleConfig;
import software.wings.security.authentication.oauth.LinkedinConfig;

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
  @JsonProperty private PortalConfig portal = new PortalConfig();
  @JsonProperty(defaultValue = "true") private boolean enableAuth = true;
  @JsonProperty(defaultValue = "50") private int jenkinsBuildQuerySize = 50;
  @JsonProperty private FileUploadLimit fileUploadLimits = new FileUploadLimit();
  @JsonProperty("backgroundScheduler") private SchedulerConfig backgroundSchedulerConfig = new SchedulerConfig();
  @JsonProperty("serviceScheduler") private SchedulerConfig serviceSchedulerConfig = new SchedulerConfig();
  @JsonProperty("watcherMetadataUrl") private String watcherMetadataUrl;
  @JsonProperty("delegateMetadataUrl") private String delegateMetadataUrl;
  @JsonProperty("awsInstanceTypes") private List<String> awsInstanceTypes;
  @JsonProperty("awsRegionIdToName") private Map<String, String> awsRegionIdToName;
  @JsonProperty("hazelcast") private HazelcastConfiguration hazelcast;
  @JsonProperty("apiUrl") private String apiUrl;
  @JsonProperty("supportEmail") private String supportEmail;
  @JsonProperty("envPath") private String envPath;
  @JsonProperty("smtp") private SmtpConfig smtpConfig;
  @JsonProperty("globalWhitelistConfig") private GlobalWhitelistConfig globalWhitelistConfig;
  @JsonProperty(defaultValue = "KUBERNETES") private DeployMode deployMode = DeployMode.KUBERNETES;
  @JsonProperty("featuresEnabled") private String featureNames;
  @JsonProperty("kubectlVersion") private String kubectlVersion;
  @JsonProperty("trialRegistrationAllowed") private boolean trialRegistrationAllowed;
  @JsonProperty("blacklistedEmailDomainsAllowed") private boolean blacklistedEmailDomainsAllowed;
  @JsonProperty("executionLogStorageMode") private DataStorageMode executionLogsStorageMode;
  @JsonProperty("fileStorageMode") private DataStorageMode fileStorageMode;
  @JsonProperty("clusterName") private String clusterName;
  @JsonProperty("marketoConfig") private MarketoConfig marketoConfig;
  @JsonProperty("segmentConfig") private SegmentConfig segmentConfig;
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
  @JsonProperty("grpcServerConfig") private GrpcServerConfig grpcServerConfig;

  @JsonProperty("disabledCache") private Set<String> disabledCache;
  @JsonProperty("techStacks") private Map<String, UrlInfo> techStackLinks;

  private int applicationPort;
  private boolean sslEnabled;

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

    super.setServerFactory(defaultServerFactory);
  }

  @Override
  public void setServerFactory(ServerFactory factory) {
    DefaultServerFactory defaultServerFactory = (DefaultServerFactory) factory;
    ((DefaultServerFactory) getServerFactory())
        .setApplicationConnectors(defaultServerFactory.getApplicationConnectors());
    ((DefaultServerFactory) getServerFactory()).setAdminConnectors(defaultServerFactory.getAdminConnectors());
    ((DefaultServerFactory) getServerFactory()).setRequestLogFactory(defaultServerFactory.getRequestLogFactory());
  }

  /**
   * Gets swagger bundle configuration.
   *
   * @return the swagger bundle configuration
   */
  public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
    SwaggerBundleConfiguration defaultSwaggerBundleConfiguration = new SwaggerBundleConfiguration();
    defaultSwaggerBundleConfiguration.setResourcePackage("software.wings.resources,software.wings.utils");
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

  private RequestLogFactory getDefaultlogbackAccessRequestLogFactory() {
    LogbackAccessRequestLogFactory logbackAccessRequestLogFactory = new LogbackAccessRequestLogFactory();
    FileAppenderFactory<IAccessEvent> fileAppenderFactory = new FileAppenderFactory<>();
    fileAppenderFactory.setArchive(true);
    fileAppenderFactory.setCurrentLogFilename("access.log");
    fileAppenderFactory.setThreshold(Level.ALL);
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
