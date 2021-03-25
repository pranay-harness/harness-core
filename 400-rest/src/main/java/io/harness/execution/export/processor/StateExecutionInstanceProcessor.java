package io.harness.execution.export.processor;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.nullIfEmpty;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;

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
    if (hasNone(stateExecutionInstanceIdToNodeMetadataMap)) {
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
    if (hasNone(stateExecutionInstances)) {
      return;
    }

    for (StateExecutionInstance stateExecutionInstance : stateExecutionInstances) {
      GraphNodeMetadata nodeMetadata = stateExecutionInstanceIdToNodeMetadataMap.get(stateExecutionInstance.getUuid());
      if (nodeMetadata == null) {
        return;
      }

      if (hasSome(stateExecutionInstance.getInterruptHistory())) {
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
    if (hasNone(executionInterrupts)) {
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
    if (hasNone(interruptIdToNodeMetadataMap)) {
      return;
    }

    List<ExecutionInterrupt> executionInterrupts =
        executionInterruptManager.listByIdsUsingSecondary(interruptIdToNodeMetadataMap.keySet());
    if (hasNone(executionInterrupts)) {
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
    if (nodeMetadata == null || hasNone(stateExecutionInterrupts)) {
      return;
    }

    List<ExecutionInterruptMetadata> executionInterruptMetadataList =
        ExecutionInterruptMetadata.fromStateExecutionInterrupts(stateExecutionInterrupts);
    if (hasNone(executionInterruptMetadataList)) {
      return;
    }

    if (hasNone(nodeMetadata.getInterruptHistory())) {
      nodeMetadata.setInterruptHistory(executionInterruptMetadataList);
    } else {
      nodeMetadata.getInterruptHistory().addAll(executionInterruptMetadataList);
    }

    // De-duplicate and sort.
    nodeMetadata.setInterruptHistory(
        nullIfEmpty(nodeMetadata.getInterruptHistory().stream().distinct().sorted().collect(Collectors.toList())));
  }
}
