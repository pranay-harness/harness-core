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
  @NotEmpty private String host = "localhost";

  @Min(1) @Max(65535) @JsonProperty private int port = 27017;

  @JsonProperty @NotEmpty private String db;

  @JsonProperty
  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  @JsonProperty
  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  @JsonProperty
  public String getDb() {
    return db;
  }

  public void setDb(String db) {
    this.db = db;
  }
}
