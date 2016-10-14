package software.wings.dl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * MongoConfig is used to store the MongoDB connection related configuration.
 *
 * @author Rishi
 */
public class MongoConfig {
  @JsonProperty(defaultValue = "localhost") @NotEmpty private String host = "localhost";

  @Min(1) @Max(65535) @JsonProperty(defaultValue = "27017") private int port = 27017;

  @JsonProperty(defaultValue = "wing") @NotEmpty private String db = "wings";

  /**
   * Gets host.
   *
   * @return the host
   */
  public String getHost() {
    return host;
  }

  /**
   * Sets host.
   *
   * @param host the host
   */
  public void setHost(String host) {
    this.host = host;
  }

  /**
   * Gets port.
   *
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * Sets port.
   *
   * @param port the port
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * Gets db.
   *
   * @return the db
   */
  public String getDb() {
    return db;
  }

  /**
   * Sets db.
   *
   * @param db the db
   */
  public void setDb(String db) {
    this.db = db;
  }
}
