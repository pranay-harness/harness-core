/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.entities.batch;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UuidAware;

import com.google.common.collect.ImmutableList;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Entity(value = "batchJobInterval", noClassnameStored = true)
@FieldNameConstants(innerTypeName = "BatchJobIntervalKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@StoreIn(DbAliases.CENG)
@OwnedBy(CE)
public final class BatchJobInterval
    implements PersistentEntity, UuidAware, CreatedAtAware, UpdatedAtAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountId_batchJobType")
                 .unique(true)
                 .field(BatchJobIntervalKeys.accountId)
                 .field(BatchJobIntervalKeys.batchJobType)
                 .build())
        .build();
  }
  @Id String uuid;
  String accountId;
  String batchJobType;
  ChronoUnit intervalUnit;
  long interval;
  long createdAt;
  long lastUpdatedAt;

  public BatchJobInterval(String accountId, String batchJobType, ChronoUnit intervalUnit, long interval) {
    this.accountId = accountId;
    this.batchJobType = batchJobType;
    this.intervalUnit = intervalUnit;
    this.interval = interval;
  }
}
