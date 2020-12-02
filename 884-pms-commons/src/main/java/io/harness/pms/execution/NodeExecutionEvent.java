package io.harness.pms.execution;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.queue.Queuable;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.CDC)
@Value
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "NodeExecutionEventKeys")
@Entity(value = "nodeExecutionEventQueue")
@Document("nodeExecutionEventQueue")
@TypeAlias("nodeExecutionEvent")
@HarnessEntity(exportable = false)
public class NodeExecutionEvent extends Queuable {
  NodeExecutionEventType eventType;
  NodeExecution nodeExecution;
}
