package software.wings.app;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;
import software.wings.dl.MongoConnectionFactory;

/**
 *  Used to load all the application configuration
 *
 * @author Rishi
 *
 */
public class MainConfiguration extends Configuration {
  @JsonProperty("mongo") private MongoConnectionFactory mongoConnectionFactory = new MongoConnectionFactory();

  @JsonProperty private PortalConfig portal;

  public MongoConnectionFactory getMongoConnectionFactory() {
    return mongoConnectionFactory;
  }

  public void setMongoConnectionFactory(MongoConnectionFactory mongoConnectionFactory) {
    this.mongoConnectionFactory = mongoConnectionFactory;
  }

  public PortalConfig getPortal() {
    return portal;
  }

  public void setPortal(PortalConfig portal) {
    this.portal = portal;
  }
}
