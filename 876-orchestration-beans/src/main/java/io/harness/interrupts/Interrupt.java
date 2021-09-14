/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.interrupts;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotation.StoreIn;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAccess;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import java.util.HashMap;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.NonNull;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import lombok.experimental.Wither;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(CDC)
@Value
@Builder
@Entity(value = "interrupts", noClassnameStored = true)
@Document(value = "interrupts")
@FieldNameConstants(innerTypeName = "InterruptKeys")
@TypeAlias("interrupt")
@StoreIn(DbAliases.PMS)
public class Interrupt implements PersistentEntity, UuidAccess {
  public enum State { REGISTERED, PROCESSING, PROCESSED_SUCCESSFULLY, PROCESSED_UNSUCCESSFULLY, DISCARDED }

  @Wither @Id @org.mongodb.morphia.annotations.Id @NotNull String uuid;
  @NonNull InterruptType type;
  @NotNull InterruptConfig interruptConfig;
  @NonNull String planExecutionId;
  String nodeExecutionId;
  StepParameters parameters;
  Map<String, String> metadata;
  @Wither @LastModifiedDate Long lastUpdatedAt;
  @Wither @CreatedDate Long createdAt;
  @NonFinal @Setter @Builder.Default State state = State.REGISTERED;
  @Wither @Version Long version;

  public AutoLogContext autoLogContext() {
    return new AutoLogContext(logContextMap(), OVERRIDE_NESTS);
  }
  private Map<String, String> logContextMap() {
    Map<String, String> logContext = new HashMap<>();
    logContext.put(InterruptKeys.planExecutionId, planExecutionId);
    logContext.put(InterruptKeys.type, type.name());
    logContext.put(InterruptKeys.nodeExecutionId, nodeExecutionId);
    return logContext;
  }
}
