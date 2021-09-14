/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.infrastructure.instance.stats;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.CreatedByAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UpdatedAtAware;
import io.harness.persistence.UpdatedByAware;
import io.harness.persistence.UuidAware;
import io.harness.validation.Update;

import software.wings.beans.EntityType;
import software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey;

import com.google.common.collect.ImmutableList;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Entity(value = "serverless-instance-stats", noClassnameStored = true)
@HarnessEntity(exportable = false)
@FieldNameConstants(innerTypeName = "ServerlessInstanceStatsKeys")
@OwnedBy(CDP)
public class ServerlessInstanceStats implements PersistentEntity, UuidAware, CreatedAtAware, CreatedByAware,
                                                UpdatedAtAware, UpdatedByAware, AccountAccess {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("accountId_timestamp_unique_idx")
                 .unique(true)
                 .field(ServerlessInstanceStatsKeys.accountId)
                 .field(ServerlessInstanceStatsKeys.timestamp)
                 .build())
        .build();
  }
  public static final InvocationCountKey DEFAULT_INVOCATION_COUNT_KEY = InvocationCountKey.LAST_30_DAYS;
  @Id @NotNull(groups = {Update.class}) private String uuid;

  private EmbeddedUser createdBy;

  @FdIndex private long createdAt;

  private EmbeddedUser lastUpdatedBy;

  @NotNull private long lastUpdatedAt;

  @NonFinal private Instant timestamp;
  @NonFinal private String accountId;
  @NonFinal private Collection<AggregateInvocationCount> aggregateCounts = new ArrayList<>();

  public ServerlessInstanceStats(
      Instant timestamp, String accountId, Collection<AggregateInvocationCount> aggregateCounts) {
    this.timestamp = timestamp;
    this.accountId = accountId;
    this.aggregateCounts = aggregateCounts;
  }

  public Collection<AggregateInvocationCount> getAggregateCounts() {
    return ImmutableList.copyOf(aggregateCounts);
  }

  @Value
  @Builder
  public static class AggregateInvocationCount {
    private EntityType entityType;
    private String name;
    private String id;
    @NonFinal private long invocationCount;
    private InvocationCountKey invocationCountKey;
  }
}
