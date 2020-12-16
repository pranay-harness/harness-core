package io.harness.cvng.analysis.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PrePersist;

@CdIndex(name = "configId_label_index", fields = { @Field("cvConfigId")
                                                   , @Field("label") })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "LogAnalysisClusterKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "logAnalysisClusters", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class LogAnalysisCluster implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  @FdIndex private String verificationTaskId;
  private Instant analysisStartTime;
  private Instant analysisEndTime;
  @FdIndex private String accountId;
  private long analysisMinute;
  private long label;
  private List<Frequency> frequencyTrend;
  private String text;
  private boolean isEvicted;
  private long firstSeenTime;

  @JsonIgnore @SchemaIgnore @FdTtlIndex private Date validUntil;

  @Data
  @Builder
  public static class Frequency {
    Integer count;
    Long timestamp;
    Double riskScore;
  }

  @PrePersist
  public void updateValidUntil() {
    if (isEvicted) {
      validUntil = Date.from(OffsetDateTime.now().plusMonths(1).toInstant());
    }
  }
}
