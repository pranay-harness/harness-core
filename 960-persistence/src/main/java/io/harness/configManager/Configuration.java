package io.harness.configManager;

import io.harness.annotation.HarnessEntity;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@FieldNameConstants(innerTypeName = "ConfigurationKeys")
@Entity(value = "configuration", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class Configuration
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, PersistentRegularIterable {
  public static final String GLOBAL_CONFIG_ID = "__GLOBAL_CONFIG_ID__";
  public static final String MATCH_ALL_VERSION = "*";
  @Id private String uuid;
  @FdIndex private long createdAt;
  @FdIndex private long lastUpdatedAt;
  private String primaryVersion;
  private Long nextIteration;

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (ConfigurationKeys.nextIteration.equals(fieldName)) {
      return this.nextIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (ConfigurationKeys.nextIteration.equals(fieldName)) {
      this.nextIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  public static final class Builder {
    String primaryVersion;

    private Builder() {}

    public static Builder aConfiguration() {
      return new Builder();
    }

    public Builder withPrimaryVersion(String primaryVersion) {
      this.primaryVersion = primaryVersion;
      return this;
    }

    public Configuration build() {
      Configuration configuration = new Configuration();
      configuration.setUuid(GLOBAL_CONFIG_ID);
      configuration.setPrimaryVersion(primaryVersion);
      return configuration;
    }
  }
}
