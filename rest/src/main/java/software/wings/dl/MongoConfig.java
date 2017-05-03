package software.wings.dl;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * MongoConfig is used to store the MongoDB connection related configuration.
 * For URI connection string format, see: https://docs.mongodb.com/manual/reference/connection-string
 *
 * @author Rishi
 */
public class MongoConfig {
  @JsonProperty(defaultValue = "mongodb://localhost:27017/wings")
  @NotEmpty
  private String uri = "mongodb://localhost:27017/wings";

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }
}
