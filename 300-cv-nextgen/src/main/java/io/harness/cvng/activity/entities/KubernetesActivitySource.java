package io.harness.cvng.activity.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO.KubernetesActivitySourceConfig;
import io.harness.cvng.beans.activity.KubernetesActivitySourceDTO.KubernetesActivitySourceConfig.KubernetesActivitySourceConfigKeys;
import io.harness.cvng.core.entities.CVConfig.CVConfigKeys;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdUniqueIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
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
  public static final String SERVICE_IDENTIFIER_KEY =
      KubernetesActivitySourceKeys.activitySourceConfigs + "." + KubernetesActivitySourceConfigKeys.serviceIdentifier;
  @Id String uuid;
  long createdAt;
  long lastUpdatedAt;

  @NotNull @FdIndex String accountId;
  @NotNull String orgIdentifier;
  @NotNull String projectIdentifier;
  @NotNull @FdUniqueIndex String identifier;
  @NotNull String name;
  @NotNull String connectorIdentifier;
  @NotNull @NotEmpty Set<KubernetesActivitySourceConfig> activitySourceConfigs;

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

  public KubernetesActivitySourceDTO toDTO() {
    return KubernetesActivitySourceDTO.builder()
        .uuid(uuid)
        .identifier(identifier)
        .name(name)
        .connectorIdentifier(connectorIdentifier)
        .activitySourceConfigs(activitySourceConfigs)
        .createdAt(createdAt)
        .lastUpdatedAt(lastUpdatedAt)
        .build();
  }
}
