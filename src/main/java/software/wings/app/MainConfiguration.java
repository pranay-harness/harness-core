package software.wings.app;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.dropwizard.Configuration;
import io.federecio.dropwizard.swagger.SwaggerBundleConfiguration;
import software.wings.dl.MongoConfig;

/**
 * Used to load all the application configuration.
 *
 * @author Rishi
 */
public class MainConfiguration extends Configuration {
  @JsonProperty("swagger") private SwaggerBundleConfiguration swaggerBundleConfiguration;

  @JsonProperty("mongo") private MongoConfig mongoConnectionFactory;

  @JsonProperty private PortalConfig portal;

  @JsonProperty private boolean enableAuth;

  @JsonProperty(defaultValue = "50") private int jenkinsBuildQuerySize;

  /**
   * Gets swagger bundle configuration.
   *
   * @return the swagger bundle configuration
   */
  public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
    return swaggerBundleConfiguration;
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
}
