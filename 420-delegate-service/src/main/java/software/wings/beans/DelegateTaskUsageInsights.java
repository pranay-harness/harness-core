/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@FieldNameConstants(innerTypeName = "DelegateTaskUsageInsightsKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Entity(value = "delegateTaskUsageInsights", noClassnameStored = true)
@HarnessEntity(exportable = false)
public class DelegateTaskUsageInsights implements PersistentEntity, UuidAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("byAcctTaskDelType")
                 .unique(true)
                 .field(DelegateTaskUsageInsightsKeys.accountId)
                 .field(DelegateTaskUsageInsightsKeys.taskId)
                 .field(DelegateTaskUsageInsightsKeys.delegateId)
                 .field(DelegateTaskUsageInsightsKeys.eventType)
                 .build())
        .build();
  }

  @Id private String uuid;
  @FdIndex private long timestamp;
  private String accountId;
  private String taskId;
  private DelegateTaskUsageInsightsEventType eventType;
  private String delegateId;
  private String delegateGroupId;
}
