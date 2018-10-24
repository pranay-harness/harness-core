package io.harness.mongo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * MongoConfig is used to store the MongoDB connection related configuration.
 * For URI connection string format, see: https://docs.mongodb.com/manual/reference/connection-string
 */

@Value
@Builder
public class MongoConfig {
  @JsonProperty(defaultValue = "mongodb://localhost:27017/wings")
  @Builder.Default
  @NotEmpty
  private String uri = "mongodb://localhost:27017/wings";

  private String locksUri;
}
