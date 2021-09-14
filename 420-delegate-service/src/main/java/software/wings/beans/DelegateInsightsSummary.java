/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateInsightsType;
import io.harness.mongo.index.CompoundMongoIndex;
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

@FieldNameConstants(innerTypeName = "DelegateInsightsSummaryKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@Entity(value = "delegateInsightsSummary", noClassnameStored = true)
@HarnessEntity(exportable = false)
@OwnedBy(DEL)
public class DelegateInsightsSummary implements PersistentEntity, UuidAware {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(CompoundMongoIndex.builder()
                 .name("byAcctGrpTimeType")
                 .field(DelegateInsightsSummaryKeys.accountId)
                 .field(DelegateInsightsSummaryKeys.delegateGroupId)
                 .field(DelegateInsightsSummaryKeys.periodStartTime)
                 .field(DelegateInsightsSummaryKeys.insightsType)
                 .build())
        .build();
  }

  @Id private String uuid;
  private String accountId;
  private DelegateInsightsType insightsType;
  private long periodStartTime;
  private long count;
  private String delegateGroupId;
}
