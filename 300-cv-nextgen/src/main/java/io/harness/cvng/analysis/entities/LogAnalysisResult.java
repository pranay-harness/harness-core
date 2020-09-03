package io.harness.cvng.analysis.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "LogAnalysisResultKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "logAnalysisResults", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class LogAnalysisResult implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  @FdIndex private String cvConfigId;
  @FdIndex private String verificationTaskId;
  private Instant analysisStartTime;
  private Instant analysisEndTime;
  @FdIndex private String accountId;
  private List<AnalysisResult> logAnalysisResults;

  @Data
  @Builder
  public static class AnalysisResult {
    private long label;
    private String tag;
    private int count;
  }
}
