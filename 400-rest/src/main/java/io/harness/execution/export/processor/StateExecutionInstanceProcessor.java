/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.processor;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.nullIfEmpty;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.export.metadata.ExecutionHistoryMetadata;
import io.harness.execution.export.metadata.ExecutionInterruptMetadata;
import io.harness.execution.export.metadata.ExecutionMetadata;
import io.harness.execution.export.metadata.GraphNodeMetadata;
import io.harness.execution.export.metadata.GraphNodeVisitor;

import software.wings.beans.StateExecutionInterrupt;
import software.wings.service.intfc.StateExecutionService;
import software.wings.sm.ExecutionInterrupt;
import software.wings.sm.ExecutionInterruptEffect;
import software.wings.sm.ExecutionInterruptManager;
import software.wings.sm.StateExecutionInstance;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@OwnedBy(CDC)
@Value
public class StateExecutionInstanceProcessor implements ExportExecutionsProcessor, GraphNodeVisitor {
  @Inject @NonFinal @Setter StateExecutionService stateExecutionService;
  @Inject @NonFinal @Setter ExecutionInterruptManager executionInterruptManager;

  Map<String, GraphNodeMetadata> stateExecutionInstanceIdToNodeMetadataMap;
  Map<String, GraphNodeMetadata> interruptIdToNodeMetadataMap;
  Map<String, ExecutionInterruptEffect> interruptIdToInterruptEffectMap;

  public StateExecutionInstanceProcessor() {
    this.stateExecutionInstanceIdToNodeMetadataMap = new HashMap<>();
    this.interruptIdToNodeMetadataMap = new HashMap<>();
    this.interruptIdToInterruptEffectMap = new HashMap<>();
  }

  public void visitExecutionMetadata(ExecutionMetadata executionMetadata) {
    executionMetadata.accept(this);
  }

  public void visitGraphNode(GraphNodeMetadata nodeMetadata) {
    if (nodeMetadata.getId() != null
        && (nodeMetadata.getInterruptHistoryCount() > 0 || nodeMetadata.getExecutionHistoryCount() > 0)) {
      stateExecutionInstanceIdToNodeMetadataMap.put(nodeMetadata.getId(), nodeMetadata);
    }
  }

  public void process() {
    if (isEmpty(stateExecutionInstanceIdToNodeMetadataMap)) {
      return;
    }

    updateInterruptRefsAndExecutionHistory();
    updateStateExecutionInstanceInterrupts();
    updateIdInterrupts();
  }

  @VisibleForTesting
  public void updateInterruptRefsAndExecutionHistory() {
    List<StateExecutionInstance> stateExecutionInstances =
        stateExecutionService.listByIdsUsingSecondary(stateExecutionInstanceIdToNodeMetadataMap.keySet());
    if (isEmpty(stateExecutionInstances)) {
      return;
    }

    for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
      GraphNodeMetadata nodeMetadata = stateExecutionInstanceIdToNodeMetadataMap.get(stateExecutionInstance.getUuid());
      if (nodeMetadata == null) {
        return;
      }

      if (isNotEmpty(stateExecutionInstance.getInterruptHistory())) {
        stateExecutionInstance.getInterruptHistory().forEach(interruptEffect -> {
          if (interruptEffect.getInterruptId() == null) {
            return;
          }

          interruptIdToNodeMetadataMap.put(interruptEffect.getInterruptId(), nodeMetadata);
          interruptIdToInterruptEffectMap.put(interruptEffect.getInterruptId(), interruptEffect);
        });
      }
      nodeMetadata.setExecutionHistory(
          ExecutionHistoryMetadata.fromStateExecutionDataList(stateExecutionInstance.getStateExecutionDataHistory()));
    }
  }

  @VisibleForTesting
  public void updateStateExecutionInstanceInterrupts() {
    List<ExecutionInterrupt> executionInterrupts = executionInterruptManager.listByStateExecutionIdsUsingSecondary(
        stateExecutionInstanceIdToNodeMetadataMap.keySet());
    if (isEmpty(executionInterrupts)) {
      return;
    }

    for (ExecutionInterrupt executionInterrupt : executionInterrupts) {
      GraphNodeMetadata nodeMetadata =
          stateExecutionInstanceIdToNodeMetadataMap.get(executionInterrupt.getStateExecutionInstanceId());
      addInterruptHistoryGraphNodeMetadata(nodeMetadata,
          Collections.singletonList(
              StateExecutionInterrupt.builder()
                  .interrupt(executionInterrupt)
                  .tookAffectAt(
                      executionInterrupt.getCreatedAt() <= 0 ? null : new Date(executionInterrupt.getCreatedAt()))
                  .build()));
    }
  }

  @VisibleForTesting
  public void updateIdInterrupts() {
    if (isEmpty(interruptIdToNodeMetadataMap)) {
      return;
    }

    List<ExecutionInterrupt> executionInterrupts =
        executionInterruptManager.listByIdsUsingSecondary(interruptIdToNodeMetadataMap.keySet());
    if (isEmpty(executionInterrupts)) {
      return;
    }

    for (ExecutionInterrupt executionInterrupt : executionInterrupts) {
      GraphNodeMetadata nodeMetadata = interruptIdToNodeMetadataMap.get(executionInterrupt.getUuid());
      ExecutionInterruptEffect interruptEffect = interruptIdToInterruptEffectMap.get(executionInterrupt.getUuid());
      if (nodeMetadata == null || interruptEffect == null) {
        continue;
      }

      addInterruptHistoryGraphNodeMetadata(nodeMetadata,
          Collections.singletonList(StateExecutionInterrupt.builder()
                                        .interrupt(executionInterrupt)
                                        .tookAffectAt(interruptEffect.getTookEffectAt())
                                        .build()));
    }
  }

  private void addInterruptHistoryGraphNodeMetadata(
      GraphNodeMetadata nodeMetadata, List<StateExecutionInterrupt> stateExecutionInterrupts) {
    if (nodeMetadata == null || isEmpty(stateExecutionInterrupts)) {
      return;
    }

    List<ExecutionInterruptMetadata> executionInterruptMetadataList =
        ExecutionInterruptMetadata.fromStateExecutionInterrupts(stateExecutionInterrupts);
    if (isEmpty(executionInterruptMetadataList)) {
      return;
    }

    if (isEmpty(nodeMetadata.getInterruptHistory())) {
      nodeMetadata.setInterruptHistory(executionInterruptMetadataList);
    } else {
      nodeMetadata.getInterruptHistory().addAll(executionInterruptMetadataList);
    }

    // De-duplicate and sort.
    nodeMetadata.setInterruptHistory(
        nullIfEmpty(nodeMetadata.getInterruptHistory().stream().distinct().sorted().collect(Collectors.toList())));
  }
}
