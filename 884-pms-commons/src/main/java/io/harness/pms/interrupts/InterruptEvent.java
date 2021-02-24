package io.harness.pms.interrupts;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotation.HarnessEntity;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.contracts.steps.StepType;
import io.harness.queue.Queuable;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "InterruptEventKeys")
@Entity(value = "interruptEventQueue")
@Document("interruptEventQueue")
@TypeAlias("interruptEvent")
@HarnessEntity(exportable = false)
public class InterruptEvent extends Queuable {
  String interruptUuid;
  StepType stepType;
  String nodeExecutionId;
  InterruptType interruptType;
  @Builder.Default String notifyId = generateUuid();
}
