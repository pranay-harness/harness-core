package io.harness.archiver.beans;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.PlanExecution;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

import java.time.OffsetDateTime;
import java.util.Date;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(PIPELINE)
@Value
@Builder
@FieldNameConstants(innerTypeName = "PlanExecutionArchiveKeys")
@Entity(value = "planExecutionArchives")
@Document("planExecutionArchives")
@TypeAlias("planExecutionArchives")
@StoreIn(DbAliases.PMS)
public class PlanExecutionArchive implements PersistentEntity, UuidAccess {
  public static final long TTL_MONTHS = 6;

  // Immutable
  @Wither @Id @org.mongodb.morphia.annotations.Id @Builder.Default String uuid = generateUuid();
  PlanExecution planExecution;

  @Builder.Default @FdTtlIndex Date validUntil = Date.from(OffsetDateTime.now().plusMonths(TTL_MONTHS).toInstant());
  @Wither @FdIndex @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long lastUpdatedAt;
  @Wither @Version Long version;

  public static PlanExecutionArchive fromPlanExecution(PlanExecution planExecution) {
    return PlanExecutionArchive.builder().uuid(generateUuid()).planExecution(planExecution).build();
  }
}
