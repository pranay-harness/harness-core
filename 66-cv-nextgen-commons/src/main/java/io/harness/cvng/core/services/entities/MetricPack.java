package io.harness.cvng.core.services.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.models.DataSourceType;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "metricPacks", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "MetricPackKeys")
@HarnessEntity(exportable = true)
public class MetricPack implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  @Indexed private String accountId;
  @Indexed private String projectId;
  @Indexed private DataSourceType dataSourceType;
  private String name;
  private Set<MetricDefinition> metrics;

  @Data
  @Builder
  @EqualsAndHashCode(of = {"name"})
  public static class MetricDefinition {
    private String name;
    private String path;
    private boolean defaultIncluded;
  }
}
