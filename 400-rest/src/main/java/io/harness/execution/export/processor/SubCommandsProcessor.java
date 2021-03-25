package io.harness.execution.export.processor;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.HasPredicate.hasNone;
import static io.harness.data.structure.HasPredicate.hasSome;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.execution.export.metadata.ActivityCommandUnitMetadata;
import io.harness.execution.export.metadata.ApprovalMetadata;
import io.harness.execution.export.metadata.ExecutionDetailsMetadata;
import io.harness.execution.export.metadata.ExecutionMetadata;
import io.harness.execution.export.metadata.GraphNodeMetadata;
import io.harness.execution.export.metadata.GraphNodeVisitor;
import io.harness.execution.export.metadata.PipelineExecutionMetadata;
import io.harness.execution.export.metadata.PipelineStageExecutionMetadata;

import software.wings.beans.command.CommandUnitDetails;
import software.wings.service.intfc.ActivityService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.NonFinal;

@OwnedBy(CDC)
@Value
public class SubCommandsProcessor implements ExportExecutionsProcessor, GraphNodeVisitor {
  @Inject @NonFinal @Setter ActivityService activityService;

  Map<String, ExecutionDetailsMetadata> activityIdToNodeMetadataMap;

  public SubCommandsProcessor() {
    this.activityIdToNodeMetadataMap = new HashMap<>();
  }

  public void visitExecutionMetadata(ExecutionMetadata executionMetadata) {
    executionMetadata.accept(this);
    addShellScriptApprovalMetadata(executionMetadata);
  }

  public void visitGraphNode(GraphNodeMetadata nodeMetadata) {
    addExecutionDetailsMetadata(nodeMetadata);
    if (hasSome(nodeMetadata.getExecutionHistory())) {
      nodeMetadata.getExecutionHistory().forEach(this::addExecutionDetailsMetadata);
    }
  }

  private void addShellScriptApprovalMetadata(ExecutionMetadata executionMetadata) {
    List<ApprovalMetadata> shellScriptApprovalMetadataList =
        SubCommandsProcessor.getShellScriptApprovalMetadataList(executionMetadata);
    if (hasNone(shellScriptApprovalMetadataList)) {
      return;
    }

    shellScriptApprovalMetadataList.forEach(this::addExecutionDetailsMetadata);
  }

  private void addExecutionDetailsMetadata(ExecutionDetailsMetadata executionDetailsMetadata) {
    if (executionDetailsMetadata != null && executionDetailsMetadata.getActivityId() != null) {
      activityIdToNodeMetadataMap.put(executionDetailsMetadata.getActivityId(), executionDetailsMetadata);
    }
  }

  public void process() {
    if (hasNone(activityIdToNodeMetadataMap)) {
      return;
    }

    Map<String, List<CommandUnitDetails>> commandUnitsMap =
        activityService.getCommandUnitsMapUsingSecondary(activityIdToNodeMetadataMap.keySet());
    if (hasNone(commandUnitsMap)) {
      return;
    }

    for (Map.Entry<String, List<CommandUnitDetails>> entry : commandUnitsMap.entrySet()) {
      ExecutionDetailsMetadata executionDetailsMetadata = activityIdToNodeMetadataMap.get(entry.getKey());
      if (executionDetailsMetadata == null) {
        continue;
      }

      executionDetailsMetadata.setSubCommands(ActivityCommandUnitMetadata.fromCommandUnitDetailsList(entry.getValue()));
    }
  }

  public static List<ApprovalMetadata> getShellScriptApprovalMetadataList(ExecutionMetadata executionMetadata) {
    if (!(executionMetadata instanceof PipelineExecutionMetadata)) {
      return Collections.emptyList();
    }

    PipelineExecutionMetadata pipelineExecutionMetadata = (PipelineExecutionMetadata) executionMetadata;
    if (hasNone(pipelineExecutionMetadata.getStages())) {
      return Collections.emptyList();
    }

    return pipelineExecutionMetadata.getStages()
        .stream()
        .filter(stage -> stage.getApprovalData() != null && stage.getApprovalData().getActivityId() != null)
        .map(PipelineStageExecutionMetadata::getApprovalData)
        .collect(toList());
  }
}
