package io.harness.mongo;

import static io.harness.mongo.IndexManager.Mode.MANUAL;
import static lombok.AccessLevel.NONE;

import com.google.common.collect.ImmutableMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.ReadPreference;
import com.mongodb.Tag;
import com.mongodb.TagSet;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.ToString;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * MongoConfig is used to store the MongoDB connection related configuration.
 * For URI connection string format, see: https://docs.mongodb.com/manual/reference/connection-string
 */

@Value
@Builder(toBuilder = true)
@ToString(onlyExplicitlyIncluded = true)
public class MongoConfig {
  @Value
  public static class ReadPref {
    String name;
    ImmutableMap<String, String> tagSet;
  }

  @JsonProperty(defaultValue = "mongodb://localhost:27017/wings")
  @Default
  @NotEmpty
  private String uri = "mongodb://localhost:27017/wings";

  @ToString.Include @Getter(NONE) private ReadPref readPref;

  private String locksUri;

  private byte[] encryptedUri;
  private byte[] encryptedLocksUri;

  @JsonProperty(defaultValue = "30000") @Default @NotEmpty private int connectTimeout = 30000;

  @JsonProperty(defaultValue = "90000") @Default @NotEmpty private int serverSelectionTimeout = 90000;

  @JsonProperty(defaultValue = "600000") @Default @NotEmpty private int maxConnectionIdleTime = 600000;

  @JsonProperty(defaultValue = "300") @Default @NotEmpty private int connectionsPerHost = 300;

  @JsonProperty(defaultValue = "MANUAL") @Default @NotEmpty private IndexManager.Mode indexManagerMode = MANUAL;

  @JsonIgnore
  public ReadPreference getReadPreference() {
    if (readPref == null || readPref.getName() == null || readPref.getName().equals("primary")) {
      return ReadPreference.primary();
    } else {
      return ReadPreference.valueOf(readPref.getName(),
          Collections.singletonList(new TagSet(Optional.ofNullable(readPref.getTagSet())
                                                   .orElse(ImmutableMap.of())
                                                   .entrySet()
                                                   .stream()
                                                   .map(entry -> new Tag(entry.getKey(), entry.getValue()))
                                                   .collect(Collectors.toList()))));
    }
  }
}
