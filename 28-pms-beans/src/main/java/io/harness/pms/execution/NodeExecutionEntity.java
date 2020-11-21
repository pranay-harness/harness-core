package io.harness.pms.execution;

import io.harness.mongo.index.FdIndex;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;

@Value
@Builder
public class NodeExecutionEntity implements PersistentEntity, UuidAccess {
  @Id @org.mongodb.morphia.annotations.Id String uuid;
  NodeExecution nodeExecution;
  @Wither @FdIndex @CreatedDate Long createdAt;
  @Wither @LastModifiedDate Long lastUpdatedAt;
  @Version Long version;
}
