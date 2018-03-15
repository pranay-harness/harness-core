package software.wings.app;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Collections.singletonList;

import com.google.common.collect.ImmutableList;

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
import software.wings.dl.MongoConfig;
import software.wings.helpers.ext.mail.SmtpConfig;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Used to load all the application configuration.
 *
 * @author Rishi
 */
public class MainConfiguration extends Configuration implements AssetsBundleConfiguration {
  @JsonProperty
  private AssetsConfiguration assetsConfiguration =
      AssetsConfiguration.builder()
          .mimeTypes(of("js", "application/json; charset=UTF-8", "zip", "application/zip"))
          .build();

  @JsonProperty("swagger") private SwaggerBundleConfiguration swaggerBundleConfiguration;
  @JsonProperty("mongo") private MongoConfig mongoConnectionFactory = new MongoConfig();
  @JsonProperty private PortalConfig portal = new PortalConfig();
  @JsonProperty(defaultValue = "true") private boolean enableAuth = true;
  @JsonProperty(defaultValue = "50") private int jenkinsBuildQuerySize = 50;
  @JsonProperty private FileUploadLimit fileUploadLimits = new FileUploadLimit();
  @JsonProperty("scheduler") private SchedulerConfig schedulerConfig = new SchedulerConfig();
  @JsonProperty("watcherMetadataUrl") private String watcherMetadataUrl;
  @JsonProperty("delegateMetadataUrl") private String delegateMetadataUrl;
  @JsonProperty("awsInstanceTypes") private List<String> awsInstanceTypes;
  @JsonProperty("awsRegionIdToName") private Map<String, String> awsRegionIdToName;
  @JsonProperty("hazelcast") private HazelcastConfiguration hazelcast;
  @JsonProperty("apiUrl") private String apiUrl;
  @JsonProperty("smtp") private SmtpConfig smtpConfig;
  @JsonProperty(defaultValue = "AWS") private DeployMode deployMode = DeployMode.AWS;
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
    return Optional.ofNullable(swaggerBundleConfiguration).orElse(defaultSwaggerBundleConfiguration);
  }

  /**
   * Sets swagger bundle configuration.
   *
   * @param swaggerBundleConfiguration the swagger bundle configuration
   */
  public void setSwaggerBundleConfiguration(SwaggerBundleConfiguration swaggerBundleConfiguration) {
    this.swaggerBundleConfiguration = swaggerBundleConfiguration;
  }

  /**
   * Gets mongo connection factory.
   *
   * @return the mongo connection factory
   */
  public MongoConfig getMongoConnectionFactory() {
    return mongoConnectionFactory;
  }

  /**
   * Sets mongo connection factory.
   *
   * @param mongoConnectionFactory the mongo connection factory
   */
  public void setMongoConnectionFactory(MongoConfig mongoConnectionFactory) {
    this.mongoConnectionFactory = mongoConnectionFactory;
  }

  /**
   * Gets portal.
   *
   * @return the portal
   */
  public PortalConfig getPortal() {
    return portal;
  }

  /**
   * Sets portal.
   *
   * @param portal the portal
   */
  public void setPortal(PortalConfig portal) {
    this.portal = portal;
  }

  /**
   * Is enable auth boolean.
   *
   * @return the boolean
   */
  public boolean isEnableAuth() {
    return enableAuth;
  }

  /**
   * Sets enable auth.
   *
   * @param enableAuth the enable auth
   */
  public void setEnableAuth(boolean enableAuth) {
    this.enableAuth = enableAuth;
  }

  /**
   * Gets jenkins build query size.
   *
   * @return the jenkins build query size
   */
  public int getJenkinsBuildQuerySize() {
    return jenkinsBuildQuerySize;
  }

  /**
   * Sets jenkins build query size.
   *
   * @param jenkinsBuildQuerySize the jenkins build query size
   */
  public void setJenkinsBuildQuerySize(int jenkinsBuildQuerySize) {
    this.jenkinsBuildQuerySize = jenkinsBuildQuerySize;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public AssetsConfiguration getAssetsConfiguration() {
    return assetsConfiguration;
  }

  /**
   * Gets file upload limit.
   *
   * @return the file upload limit
   */
  public FileUploadLimit getFileUploadLimits() {
    return fileUploadLimits;
  }

  /**
   * Sets file upload limit.
   *
   * @param fileUploadLimits the file upload limit
   */
  public void setFileUploadLimits(FileUploadLimit fileUploadLimits) {
    this.fileUploadLimits = fileUploadLimits;
  }

  /**
   * Getter for property 'delegateMetadataUrl'.
   *
   * @return Value for property 'delegateMetadataUrl'.
   */
  public String getDelegateMetadataUrl() {
    return delegateMetadataUrl;
  }

  /**
   * Setter for property 'delegateMetadataUrl'.
   *
   * @param delegateMetadataUrl Value to set for property 'delegateMetadataUrl'.
   */
  public void setDelegateMetadataUrl(String delegateMetadataUrl) {
    this.delegateMetadataUrl = delegateMetadataUrl;
  }

  public String getWatcherMetadataUrl() {
    return watcherMetadataUrl;
  }

  public void setWatcherMetadataUrl(String watcherMetadataUrl) {
    this.watcherMetadataUrl = watcherMetadataUrl;
  }

  public String getApiUrl() {
    return apiUrl;
  }

  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  public DeployMode getDeployMode() {
    return deployMode;
  }

  public void setDeployMode(DeployMode deployMode) {
    this.deployMode = deployMode;
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

  public SchedulerConfig getSchedulerConfig() {
    return schedulerConfig;
  }

  public void setSchedulerConfig(SchedulerConfig schedulerConfig) {
    this.schedulerConfig = schedulerConfig;
  }

  public List<String> getAwsInstanceTypes() {
    return awsInstanceTypes;
  }

  public void setAwsInstanceTypes(List<String> awsInstanceTypes) {
    this.awsInstanceTypes = awsInstanceTypes;
  }

  public Map<String, String> getAwsRegionIdToName() {
    return awsRegionIdToName;
  }

  public void setAwsRegionIdToName(Map<String, String> awsRegionIdToName) {
    this.awsRegionIdToName = awsRegionIdToName;
  }

  /**
   * Getter for property 'hazelcast'.
   *
   * @return Value for property 'hazelcast'.
   */
  public HazelcastConfiguration getHazelcast() {
    return hazelcast;
  }

  /**
   * Setter for property 'hazelcast'.
   *
   * @param hazelcast Value to set for property 'hazelcast'.
   */
  public void setHazelcast(HazelcastConfiguration hazelcast) {
    this.hazelcast = hazelcast;
  }

  public SmtpConfig getSmtpConfig() {
    return smtpConfig;
  }

  public void setSmtpConfig(SmtpConfig smtpConfig) {
    this.smtpConfig = smtpConfig;
  }

  public int getApplicationPort() {
    return applicationPort;
  }

  public void setApplicationPort(int applicationPort) {
    this.applicationPort = applicationPort;
  }

  public boolean isSslEnabled() {
    return sslEnabled;
  }

  public void setSslEnabled(boolean sslEnabled) {
    this.sslEnabled = sslEnabled;
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
