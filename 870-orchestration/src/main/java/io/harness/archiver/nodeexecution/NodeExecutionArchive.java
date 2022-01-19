package io.harness.archiver.nodeexecution;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.NodeExecution;
import io.harness.mongo.index.FdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
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

import java.time.OffsetDateTime;
import java.util.Date;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

@OwnedBy(PIPELINE)
@Value
@Builder
@FieldNameConstants(innerTypeName = "NodeExecutionArchiveKeys")
@Entity(value = "nodeExecutionArchives", noClassnameStored = true)
@Document("nodeExecutionArchives")
@TypeAlias("nodeExecutionArchives")
@StoreIn(DbAliases.PMS)
public class NodeExecutionArchive implements PersistentEntity, UuidAccess {
  public static final long TTL_MONTHS = 6;

  // Immutable
  @Wither @Id @org.mongodb.morphia.annotations.Id @Builder.Default String uuid = generateUuid();
  NodeExecution nodeExecution;

  @Builder.Default @FdTtlIndex Date validUntil = Date.from(OffsetDateTime.now().plusMonths(TTL_MONTHS).toInstant());
  @Wither @FdIndex @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long lastUpdatedAt;
  @Wither @Version Long version;

  public static NodeExecutionArchive fromNodeExecution(NodeExecution nodeExecution) {
    return NodeExecutionArchive.builder().uuid(generateUuid()).nodeExecution(nodeExecution).build();
  }
}
