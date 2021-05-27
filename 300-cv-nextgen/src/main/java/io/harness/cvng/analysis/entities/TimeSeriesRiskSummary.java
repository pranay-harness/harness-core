package io.harness.cvng.analysis.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.cvng.analysis.beans.Risk;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "TimeSeriesRiskSummaryKeys")
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "timeseriesRiskSummary", noClassnameStored = true)
@HarnessEntity(exportable = false)
@StoreIn(DbAliases.CVNG)
public final class TimeSeriesRiskSummary implements PersistentEntity, UuidAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("query_idx")
                 .field(TimeSeriesRiskSummaryKeys.analysisEndTime)
                 .field(TimeSeriesRiskSummaryKeys.verificationTaskId)
                 .build())
        .build();
  }

  @Id private String uuid;
  @NotEmpty private String verificationTaskId;
  @NotEmpty private Instant analysisStartTime;
  @NotEmpty private Instant analysisEndTime;
  private List<TransactionMetricRisk> transactionMetricRiskList;
  private double overallRisk;

  public List<TransactionMetricRisk> getTransactionMetricRiskList() {
    if (transactionMetricRiskList == null) {
      return new ArrayList<>();
    }
    return transactionMetricRiskList;
  }

  @Data
  @Builder
  @FieldNameConstants(innerTypeName = "TransactionMetricRiskKeys")
  public static class TransactionMetricRisk {
    private String transactionName;
    private String metricName;
    private Integer metricRisk;
    private double metricScore;
    private boolean longTermPattern;
    private long lastSeenTime;
    public Risk getMetricRisk() {
      return Risk.valueOf(metricRisk);
    }
    public boolean isAnomalous() {
      return getMetricRisk().isGreaterThanEq(Risk.MEDIUM);
    }
  }
}
