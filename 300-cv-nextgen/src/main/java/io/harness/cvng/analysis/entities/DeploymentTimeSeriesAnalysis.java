package io.harness.cvng.analysis.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "DeploymentTimeSeriesAnalysisKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Entity(value = "deploymentTimeSeriesAnalyses", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class DeploymentTimeSeriesAnalysis
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;
  private String accountId;
  private String verificationTaskId;
  private Instant startTime;
  private Instant endTime;
  private int risk;
  private Double score;
  private List<DeploymentTimeSeriesAnalysisDTO.HostInfo> hostSummaries;
  private List<DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData> transactionMetricSummaries;

  public List<DeploymentTimeSeriesAnalysisDTO.TransactionMetricHostData> getTransactionMetricSummaries() {
    if (this.transactionMetricSummaries == null) {
      return Collections.emptyList();
    }
    return transactionMetricSummaries;
  }

  public List<DeploymentTimeSeriesAnalysisDTO.HostInfo> getHostSummaries() {
    if (hostSummaries == null) {
      return Collections.emptyList();
    }
    return hostSummaries;
  }
}
