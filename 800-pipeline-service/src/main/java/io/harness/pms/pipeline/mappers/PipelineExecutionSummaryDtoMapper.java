package io.harness.pms.pipeline.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.execution.StagesExecutionMetadata;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.GraphLayoutNodeDTO;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionSummaryDTO;

import java.util.ArrayList;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PIPELINE)
public class PipelineExecutionSummaryDtoMapper {
  public PipelineExecutionSummaryDTO toDto(
      PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity, EntityGitDetails entityGitDetails) {
    entityGitDetails = updateEntityGitDetails(entityGitDetails);
    Map<String, GraphLayoutNodeDTO> layoutNodeDTOMap = pipelineExecutionSummaryEntity.getLayoutNodeMap();
    String startingNodeId = pipelineExecutionSummaryEntity.getStartingNodeId();
    StagesExecutionMetadata stagesExecutionMetadata = pipelineExecutionSummaryEntity.getStagesExecutionMetadata();
    return PipelineExecutionSummaryDTO.builder()
        .name(pipelineExecutionSummaryEntity.getName())
        .createdAt(pipelineExecutionSummaryEntity.getCreatedAt())
        .layoutNodeMap(layoutNodeDTOMap)
        .moduleInfo(pipelineExecutionSummaryEntity.getModuleInfo())
        .startingNodeId(startingNodeId)
        .planExecutionId(pipelineExecutionSummaryEntity.getPlanExecutionId())
        .pipelineIdentifier(pipelineExecutionSummaryEntity.getPipelineIdentifier())
        .startTs(pipelineExecutionSummaryEntity.getStartTs())
        .endTs(pipelineExecutionSummaryEntity.getEndTs())
        .status(pipelineExecutionSummaryEntity.getStatus())
        .executionTriggerInfo(pipelineExecutionSummaryEntity.getExecutionTriggerInfo())
        .executionErrorInfo(pipelineExecutionSummaryEntity.getExecutionErrorInfo())
        .successfulStagesCount(getStagesCount(layoutNodeDTOMap, startingNodeId, ExecutionStatus.SUCCESS))
        .failedStagesCount(getStagesCount(layoutNodeDTOMap, startingNodeId, ExecutionStatus.FAILED))
        .runningStagesCount(getStagesCount(layoutNodeDTOMap, startingNodeId, ExecutionStatus.RUNNING))
        .totalStagesCount(getStagesCount(layoutNodeDTOMap, startingNodeId))
        .runSequence(pipelineExecutionSummaryEntity.getRunSequence())
        .tags(pipelineExecutionSummaryEntity.getTags())
        .modules(EmptyPredicate.isEmpty(pipelineExecutionSummaryEntity.getModules())
                ? new ArrayList<>()
                : pipelineExecutionSummaryEntity.getModules())
        .gitDetails(entityGitDetails)
        .governanceMetadata(pipelineExecutionSummaryEntity.getGovernanceMetadata())
        .isStagesExecution(stagesExecutionMetadata != null && stagesExecutionMetadata.isStagesExecution())
        .stagesExecuted(stagesExecutionMetadata == null ? null : stagesExecutionMetadata.getStageIdentifiers())
        .build();
  }

  public int getStagesCount(
      Map<String, GraphLayoutNodeDTO> layoutNodeDTOMap, String startingNodeId, ExecutionStatus executionStatus) {
    if (startingNodeId == null) {
      return 0;
    }
    int count = 0;
    GraphLayoutNodeDTO nodeDTO = layoutNodeDTOMap.get(startingNodeId);
    if (!nodeDTO.getNodeType().equals("parallel") && nodeDTO.getStatus().equals(executionStatus)) {
      count++;
    } else if (nodeDTO.getNodeType().equals("parallel")) {
      for (String child : nodeDTO.getEdgeLayoutList().getCurrentNodeChildren()) {
        if (layoutNodeDTOMap.get(child).getStatus().equals(executionStatus)) {
          count++;
        }
      }
    }
    if (nodeDTO.getEdgeLayoutList().getNextIds().isEmpty()) {
      return count;
    }
    return count + getStagesCount(layoutNodeDTOMap, nodeDTO.getEdgeLayoutList().getNextIds().get(0), executionStatus);
  }
  public int getStagesCount(Map<String, GraphLayoutNodeDTO> layoutNodeDTOMap, String startingNodeId) {
    if (startingNodeId == null) {
      return 0;
    }
    int count = 0;
    GraphLayoutNodeDTO nodeDTO = layoutNodeDTOMap.get(startingNodeId);
    if (!nodeDTO.getNodeType().equals("parallel")) {
      count++;
    } else if (nodeDTO.getNodeType().equals("parallel")) {
      count += nodeDTO.getEdgeLayoutList().getCurrentNodeChildren().size();
    }
    if (nodeDTO.getEdgeLayoutList().getNextIds().isEmpty()) {
      return count;
    }
    return count + getStagesCount(layoutNodeDTOMap, nodeDTO.getEdgeLayoutList().getNextIds().get(0));
  }

  private EntityGitDetails updateEntityGitDetails(EntityGitDetails entityGitDetails) {
    if (entityGitDetails == null) {
      return null;
    }
    String rootFolder = entityGitDetails.getRootFolder();
    String filePath = entityGitDetails.getFilePath();
    if (rootFolder == null && filePath == null) {
      return entityGitDetails;
    } else if (rootFolder == null || filePath == null || rootFolder.equals(GitSyncConstants.DEFAULT)
        || filePath.equals(GitSyncConstants.DEFAULT)) {
      return EntityGitDetails.builder()
          .branch(entityGitDetails.getBranch())
          .repoIdentifier(entityGitDetails.getRepoIdentifier())
          .objectId(entityGitDetails.getObjectId())
          .build();
    }
    return entityGitDetails;
  }
}
