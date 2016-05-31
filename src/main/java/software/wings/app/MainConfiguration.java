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

  public SwaggerBundleConfiguration getSwaggerBundleConfiguration() {
    return swaggerBundleConfiguration;
  }

  public void setSwaggerBundleConfiguration(SwaggerBundleConfiguration swaggerBundleConfiguration) {
    this.swaggerBundleConfiguration = swaggerBundleConfiguration;
  }

  public MongoConfig getMongoConnectionFactory() {
    return mongoConnectionFactory;
  }

  public void setMongoConnectionFactory(MongoConfig mongoConnectionFactory) {
    this.mongoConnectionFactory = mongoConnectionFactory;
  }

  public PortalConfig getPortal() {
    return portal;
  }

  public void setPortal(PortalConfig portal) {
    this.portal = portal;
  }

  public boolean isEnableAuth() {
    return enableAuth;
  }

  public void setEnableAuth(boolean enableAuth) {
    this.enableAuth = enableAuth;
  }

  public int getJenkinsBuildQuerySize() {
    return jenkinsBuildQuerySize;
  }

  public void setJenkinsBuildQuerySize(int jenkinsBuildQuerySize) {
    this.jenkinsBuildQuerySize = jenkinsBuildQuerySize;
  }
}
