package io.harness.cvng.analysis.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.Field;
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

@CdIndex(name = "configId_time_index",
    fields = { @Field("cvConfigId")
               , @Field("analysisStartTime"), @Field("analysisEndTime") })

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
  @FdIndex private String verificationTaskId;
  private Instant analysisStartTime;
  private Instant analysisEndTime;
  @FdIndex private String accountId;
  private double score;
  private List<AnalysisResult> logAnalysisResults;

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "AnalysisResultKeys")
  public static class AnalysisResult {
    private long label;
    private LogAnalysisTag tag;
    private int count;
  }

  public enum LogAnalysisTag {
    KNOWN(0),
    UNEXPECTED(1),
    UNKNOWN(2);

    private Integer severity;

    LogAnalysisTag(int severity) {
      this.severity = severity;
    }

    public boolean isMoreSevereThan(LogAnalysisTag other) {
      return this.severity > other.severity;
    }
  }
}
