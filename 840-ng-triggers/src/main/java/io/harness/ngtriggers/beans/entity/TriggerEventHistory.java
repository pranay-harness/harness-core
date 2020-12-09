package io.harness.ngtriggers.beans.entity;

import io.harness.annotation.HarnessEntity;
import io.harness.mongo.index.CompoundMongoIndex;
import io.harness.mongo.index.MongoIndex;
import io.harness.mongo.index.SortCompoundMongoIndex;
import io.harness.ngtriggers.beans.target.pipeline.TargetExecutionSummary;
import io.harness.persistence.PersistentEntity;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@FieldNameConstants(innerTypeName = "TriggerEventHistoryKeys")
@Entity(value = "triggerEventHistory", noClassnameStored = true)
@Document("triggerEventHistory")
@TypeAlias("triggerEventHistory")
@HarnessEntity(exportable = true)
public class TriggerEventHistory implements PersistentEntity {
  public static List<MongoIndex> mongoIndexes() {
    return ImmutableList.<MongoIndex>builder()
        .add(SortCompoundMongoIndex.builder()
                 .name("las")
                 .field(TriggerEventHistoryKeys.accountId)
                 .field(TriggerEventHistoryKeys.orgIdentifier)
                 .field(TriggerEventHistoryKeys.projectIdentifier)
                 .field(TriggerEventHistoryKeys.triggerIdentifier)
                 .descSortField(TriggerEventHistoryKeys.createdAt)
                 .build())
        .build();
  }

  @Id @org.mongodb.morphia.annotations.Id String uuid;
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String eventCorrelationId;
  String payload;
  Long eventCreatedAt;
  String finalStatus;
  String message;
  String planExecutionId;
  boolean exceptionOccurred;
  String triggerIdentifier;
  TargetExecutionSummary targetExecutionSummary;

  @CreatedDate Long createdAt;
}
