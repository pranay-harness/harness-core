/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionInterruptType;
import io.harness.execution.export.ExportExecutionsUtils;

import software.wings.beans.StateExecutionInterrupt;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.annotations.VisibleForTesting;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ExecutionInterruptMetadata implements Comparable<ExecutionInterruptMetadata> {
  @Getter(onMethod = @__(@JsonIgnore)) @JsonIgnore @EqualsAndHashCode.Include String id;
  ExecutionInterruptType interruptType;

  EmbeddedUserMetadata createdBy;
  ZonedDateTime createdAt;

  ZonedDateTime tookEffectAt;

  public int compareTo(ExecutionInterruptMetadata other) {
    if (other == null || other.getTookEffectAt() == null) {
      return 1;
    } else if (tookEffectAt == null) {
      return -1;
    }

    return tookEffectAt.compareTo(other.getTookEffectAt());
  }

  public static List<ExecutionInterruptMetadata> fromStateExecutionInterrupts(
      List<StateExecutionInterrupt> stateExecutionInterrupts) {
    return MetadataUtils.map(stateExecutionInterrupts, ExecutionInterruptMetadata::fromStateExecutionInterrupt);
  }

  @VisibleForTesting
  static ExecutionInterruptMetadata fromStateExecutionInterrupt(StateExecutionInterrupt stateExecutionInterrupt) {
    if (stateExecutionInterrupt == null || stateExecutionInterrupt.getInterrupt() == null
        || stateExecutionInterrupt.getInterrupt().getUuid() == null) {
      return null;
    }

    return ExecutionInterruptMetadata.builder()
        .id(stateExecutionInterrupt.getInterrupt().getUuid())
        .interruptType(stateExecutionInterrupt.getInterrupt().getExecutionInterruptType())
        .createdBy(EmbeddedUserMetadata.fromEmbeddedUser(stateExecutionInterrupt.getInterrupt().getCreatedBy()))
        .createdAt(stateExecutionInterrupt.getInterrupt().getCreatedAt() <= 0
                ? null
                : ExportExecutionsUtils.prepareZonedDateTime(stateExecutionInterrupt.getInterrupt().getCreatedAt()))
        .tookEffectAt(stateExecutionInterrupt.getTookAffectAt() == null
                ? null
                : ExportExecutionsUtils.prepareZonedDateTime(
                    stateExecutionInterrupt.getTookAffectAt().toInstant().toEpochMilli()))
        .build();
  }
}
