/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.dashboard.entities;

import io.harness.annotation.HarnessEntity;
import io.harness.annotation.StoreIn;
import io.harness.cvng.analysis.entities.HealthVerificationPeriod;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.SchemaIgnore;
import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Builder
@EqualsAndHashCode(callSuper = false, exclude = {"validUntil"})
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldNameConstants(innerTypeName = "HealthVerificationHeatMapKeys")
@Entity(value = "healthVerificationHeatMaps", noClassnameStored = true)
@HarnessEntity(exportable = false)
@StoreIn(DbAliases.CVNG)
public final class HealthVerificationHeatMap implements UuidAware, PersistentEntity, CreatedAtAware, UpdatedAtAware {
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
                 .name("fetchIdx")
                 .field(HealthVerificationHeatMapKeys.category)
                 .field(HealthVerificationHeatMapKeys.healthVerificationPeriod)
                 .field(HealthVerificationHeatMapKeys.aggregationLevel)
                 .field(HealthVerificationHeatMapKeys.activityId)
                 .build())
        .build();
  }

  @Id private String uuid;
  private String aggregationId;
  // TODO: remove this if possible.
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

  @FdIndex private long createdAt;
  @FdIndex private long lastUpdatedAt;

  @JsonIgnore
  @SchemaIgnore
  @Builder.Default
  @FdTtlIndex
  private Date validUntil = Date.from(OffsetDateTime.now().plusMonths(6).toInstant());

  public enum AggregationLevel {
    VERIFICATION_TASK,
    ACTIVITY;
  }
}
