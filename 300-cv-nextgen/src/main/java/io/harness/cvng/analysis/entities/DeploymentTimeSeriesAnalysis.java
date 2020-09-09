package io.harness.cvng.analysis.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO.HostSummary;
import io.harness.cvng.analysis.beans.DeploymentTimeSeriesAnalysisDTO.ResultSummary;
import io.harness.persistence.AccountAccess;
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
  private ResultSummary resultSummary;
  private List<HostSummary> hostSummaries;
}
