package io.harness.cvng.activity.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@FieldNameConstants(innerTypeName = "KubernetesActivitySourceKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "kubernetesActivitySources", noClassnameStored = true)
@HarnessEntity(exportable = true)
public class KubernetesActivitySource
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, PersistentRegularIterable {
  @Id String uuid;
  long createdAt;
  long lastUpdatedAt;

  @NotNull @FdIndex String accountId;
  @NotNull String orgIdentifier;
  @NotNull String projectIdentifier;
  @NotNull String connectorIdentifier;
  @NotNull String serviceIdentifier;
  @NotNull String envIdentifier;
  @NotNull String namespace;
  @NotNull String clusterName;
  String workloadName;

  String dataCollectionTaskId;
  @FdIndex Long dataCollectionTaskIteration;

  @Override
  public void updateNextIteration(String fieldName, long nextIteration) {
    if (CVConfigKeys.dataCollectionTaskIteration.equals(fieldName)) {
      this.dataCollectionTaskIteration = nextIteration;
      return;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }

  @Override
  public Long obtainNextIteration(String fieldName) {
    if (CVConfigKeys.dataCollectionTaskIteration.equals(fieldName)) {
      return this.dataCollectionTaskIteration;
    }
    throw new IllegalArgumentException("Invalid fieldName " + fieldName);
  }
}
