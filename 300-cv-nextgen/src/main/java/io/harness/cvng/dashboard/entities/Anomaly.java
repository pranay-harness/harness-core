package io.harness.cvng.dashboard.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.reinert.jjschema.SchemaIgnore;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "AnomalyKeys")
@Entity(value = "anomalies", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class Anomaly implements UuidAware, CreatedAtAware, AccountAccess, PersistentEntity {
  @Id private String uuid;
  @FdIndex private String accountId;
  private String serviceIdentifier;
  private String envIdentifier;
  private String projectIdentifier;
  private CVMonitoringCategory category;
  private Instant anomalyStartTime;
  private Instant anomalyEndTime;
  private AnomalyStatus status;
  @Singular("addAnomalyDetail") private Set<AnomalyDetail> anomalyDetails;
  private Set<String> anomalousConfigIds;

  @FdIndex private long createdAt;

  @JsonIgnore
  @SchemaIgnore
  @Builder.Default
  @FdTtlIndex
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(31).toInstant());

  @Value
  @Builder
  @EqualsAndHashCode(of = {"cvConfigId", "reportedTime"})
  public static class AnomalyDetail {
    Instant reportedTime;
    String cvConfigId;
    @Singular("addAnomalousMetric") Set<AnomalousMetric> anomalousMetrics;
  }

  @Data
  @Builder
  @EqualsAndHashCode(of = {"groupName", "metricName"})
  public static class AnomalousMetric {
    String groupName;
    String metricName;
    Double riskScore;
  }

  public enum AnomalyStatus {
    CLOSED("Closed"),
    OPEN("Open");

    private String displayName;
    AnomalyStatus(String displayName) {
      this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
      return displayName;
    }
  }
}
