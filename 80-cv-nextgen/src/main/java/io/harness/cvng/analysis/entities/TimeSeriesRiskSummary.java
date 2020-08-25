package io.harness.cvng.analysis.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TimeSeriesRiskSummaryKeys")
@EqualsAndHashCode(callSuper = false, exclude = {"transactionMetricSums", "compressedMetricSums"})
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "timeseriesRiskSummary", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class TimeSeriesRiskSummary implements PersistentEntity, UuidAware {
  @Id private String uuid;
  @NotEmpty @FdIndex private String cvConfigId;
  @FdIndex private String verificationTaskId;
  @NotEmpty @FdIndex private Instant analysisStartTime;
  @NotEmpty @FdIndex private Instant analysisEndTime;
  private List<TransactionMetricRisk> transactionMetricRiskList;

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "TransactionMetricRiskKeys")
  public static class TransactionMetricRisk {
    private String transactionName;
    private String metricName;
    private int metricRisk;
    private double metricScore;
    private boolean longTermPattern;
    private long lastSeenTime;
  }
}
