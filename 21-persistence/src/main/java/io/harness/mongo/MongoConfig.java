package io.harness.mongo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * MongoConfig is used to store the MongoDB connection related configuration.
 * For URI connection string format, see: https://docs.mongodb.com/manual/reference/connection-string
 */

@Value
@Builder
@ToString(onlyExplicitlyIncluded = true)
public class MongoConfig {
  @JsonProperty(defaultValue = "mongodb://localhost:27017/wings")
  @Builder.Default
  @NotEmpty
  private String uri = "mongodb://localhost:27017/wings";

  private String locksUri;

  private byte[] encryptedUri;
  private byte[] encryptedLocksUri;

  private int connectTimeout = 30000;
  private int serverSelectionTimeout = 90000;
  private int maxConnectionIdleTime = 600000;
  private int connectionsPerHost = 300;
}
