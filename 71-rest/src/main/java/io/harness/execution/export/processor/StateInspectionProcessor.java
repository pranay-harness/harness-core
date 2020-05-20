package io.harness.execution.export.processor;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.CollectionUtils.nullIfEmptyMap;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.export.metadata.ExecutionMetadata;
import io.harness.execution.export.metadata.GraphNodeMetadata;
import io.harness.execution.export.metadata.GraphNodeVisitor;
import io.harness.state.inspection.ExpressionVariableUsage;
import io.harness.state.inspection.StateInspection;
import io.harness.state.inspection.StateInspectionData;
import io.harness.state.inspection.StateInspectionService;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Value
public class StateInspectionProcessor implements ExportExecutionsProcessor, GraphNodeVisitor {
  @Inject @NonFinal @Setter StateInspectionService stateInspectionService;

  Map<String, GraphNodeMetadata> stateExecutionInstanceIdToNodeMetadataMap;

  public StateInspectionProcessor() {
    this.stateExecutionInstanceIdToNodeMetadataMap = new HashMap<>();
  }

  public void visitExecutionMetadata(ExecutionMetadata executionMetadata) {
    executionMetadata.accept(this);
  }

  public void visitGraphNode(GraphNodeMetadata nodeMetadata) {
    if (nodeMetadata.getId() != null && nodeMetadata.isHasInspection()) {
      stateExecutionInstanceIdToNodeMetadataMap.put(nodeMetadata.getId(), nodeMetadata);
    }
  }

  public void process() {
    if (isEmpty(stateExecutionInstanceIdToNodeMetadataMap)) {
      return;
    }

    List<StateInspection> stateInspections =
        stateInspectionService.listUsingSecondary(stateExecutionInstanceIdToNodeMetadataMap.keySet());
    if (isEmpty(stateInspections)) {
      return;
    }

    for (StateInspection stateInspection : stateInspections) {
      updateGraphNodeMetadata(
          stateExecutionInstanceIdToNodeMetadataMap.get(stateInspection.getStateExecutionInstanceId()),
          stateInspection);
    }
  }

  private void updateGraphNodeMetadata(GraphNodeMetadata nodeMetadata, StateInspection stateInspection) {
    if (nodeMetadata == null || stateInspection == null || isEmpty(stateInspection.getData())) {
      return;
    }

    StateInspectionData stateInspectionData = stateInspection.getData().getOrDefault("expressionVariableUsage", null);
    if (!(stateInspectionData instanceof ExpressionVariableUsage)) {
      return;
    }

    ExpressionVariableUsage expressionVariableUsage = (ExpressionVariableUsage) stateInspectionData;
    if (isEmpty(expressionVariableUsage.getVariables())) {
      return;
    }

    nodeMetadata.setExecutionContext(
        nullIfEmptyMap(expressionVariableUsage.getVariables()
                           .stream()
                           .filter(variable -> variable.getExpression() != null && variable.getValue() != null)
                           .collect(Collectors.toMap(
                               ExpressionVariableUsage.Item::getExpression, ExpressionVariableUsage.Item::getValue))));
  }
}
