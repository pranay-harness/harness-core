package io.harness.cvng.analysis.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.analysis.beans.LogClusterLevel;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "ClusteredLogKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "clusteredLog", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class ClusteredLog implements PersistentEntity, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  @NotEmpty @FdIndex private String cvConfigId;
  private LogClusterLevel clusterLevel;
  private String log;
  private Instant logTime;
  private String host;
  private String clusterLabel;
  private int clusterCount;
}
