package io.harness.cvng.alert.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.Instant;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "AlertRuleAnomalyKeys")
@Entity(value = "alertRuleAnomalies", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class AlertRuleAnomaly implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware {
  @Id private String uuid;
  private long createdAt;
  private long lastUpdatedAt;

  @FdIndex private String accountId;
  private String orgIdentifier;
  private String projectIdentifier;
  private String serviceIdentifier;
  private String envIdentifier;

  private CVMonitoringCategory category;

  private Instant anomalyStartTime;
  private Instant lastNotificationSentAt;

  private AlertRuleAnomalyStatus alertRuleAnomalyStatus;

  public enum AlertRuleAnomalyStatus {
    CLOSED("Closed"),
    OPEN("Open");

    private String displayName;
    AlertRuleAnomalyStatus(String displayName) {
      this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
      return displayName;
    }
  }
}
