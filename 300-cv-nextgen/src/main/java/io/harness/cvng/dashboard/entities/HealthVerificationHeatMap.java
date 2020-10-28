package io.harness.cvng.dashboard.entities;

import com.google.common.collect.ImmutableList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotation.HarnessEntity;
import io.harness.cvng.analysis.entities.HealthVerificationPeriod;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "HealthVerificationHeatMapKeys")
@Entity(value = "healthVerificationHeatMaps", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class HealthVerificationHeatMap implements UuidAware, PersistentEntity, CreatedAtAware, UpdatedAtAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("insertionIdx")
                 .field(HealthVerificationHeatMapKeys.category)
                 .field(HealthVerificationHeatMapKeys.healthVerificationPeriod)
                 .field(HealthVerificationHeatMapKeys.aggregationLevel)
                 .field(HealthVerificationHeatMapKeys.aggregationId)
                 .build())
        .add(CompoundMongoIndex.builder()
                 .name("insertionIdx")
                 .field(HealthVerificationHeatMapKeys.category)
                 .field(HealthVerificationHeatMapKeys.healthVerificationPeriod)
                 .field(HealthVerificationHeatMapKeys.aggregationLevel)
                 .field(HealthVerificationHeatMapKeys.activityId)
                 .build())
        .build();
  }

  @Id private String uuid;
  private String aggregationId;
  @FdIndex private String activityId;
  private String serviceIdentifier;
  private String envIdentifier;
  private String projectIdentifier;
  private CVMonitoringCategory category;
  private HealthVerificationPeriod healthVerificationPeriod;

  private Instant startTime;
  private Instant endTime;
  private String accountId;
  private double riskScore;
  private AggregationLevel aggregationLevel;

  private long createdAt;
  private long lastUpdatedAt;

  public enum AggregationLevel {
    VERIFICATION_TASK,
    ACTIVITY;
  }
}
