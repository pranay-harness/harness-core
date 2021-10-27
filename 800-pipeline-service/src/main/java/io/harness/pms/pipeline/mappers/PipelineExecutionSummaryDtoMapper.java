package io.harness.pms.pipeline.mappers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.StagesExecutionMetadata;
import io.harness.gitsync.interceptor.GitSyncConstants;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.pms.execution.ExecutionStatus;
import io.harness.pms.pipeline.PipelineEntity;
import io.harness.pms.pipeline.service.PMSPipelineService;
import io.harness.pms.plan.execution.beans.PipelineExecutionSummaryEntity;
import io.harness.pms.plan.execution.beans.dto.GraphLayoutNodeDTO;
import io.harness.pms.plan.execution.beans.dto.PipelineExecutionSummaryDTO;
import io.harness.pms.stages.BasicStageInfo;
import io.harness.pms.stages.StageExecutionSelectorHelper;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@OwnedBy(PIPELINE)
public class PipelineExecutionSummaryDtoMapper {
  @Inject PMSPipelineService pipelineService;

  public PipelineExecutionSummaryDTO toDto(
      PipelineExecutionSummaryEntity pipelineExecutionSummaryEntity, EntityGitDetails entityGitDetails) {
    entityGitDetails = updateEntityGitDetails(entityGitDetails);
    Map<String, GraphLayoutNodeDTO> layoutNodeDTOMap = pipelineExecutionSummaryEntity.getLayoutNodeMap();
    String startingNodeId = pipelineExecutionSummaryEntity.getStartingNodeId();
    StagesExecutionMetadata stagesExecutionMetadata = pipelineExecutionSummaryEntity.getStagesExecutionMetadata();
    boolean isStagesExecution = stagesExecutionMetadata != null && stagesExecutionMetadata.isStagesExecution();
    List<String> stageIdentifiers =
        stagesExecutionMetadata == null ? null : stagesExecutionMetadata.getStageIdentifiers();
    Map<String, String> stagesExecutedNames = null;
    if (EmptyPredicate.isNotEmpty(stageIdentifiers)) {
      stagesExecutedNames = getStageNames(pipelineExecutionSummaryEntity.getAccountId(),
          pipelineExecutionSummaryEntity.getOrgIdentifier(), pipelineExecutionSummaryEntity.getProjectIdentifier(),
          pipelineExecutionSummaryEntity.getPipelineIdentifier(), stageIdentifiers);
    }
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
        .canRetry(pipelineExecutionSummaryEntity.isLatestExecution())
        .showRetryHistory(!pipelineExecutionSummaryEntity.isLatestExecution()
            || !pipelineExecutionSummaryEntity.getPlanExecutionId().equals(
                pipelineExecutionSummaryEntity.getRetryExecutionMetadata().getRootExecutionId()))
        .governanceMetadata(pipelineExecutionSummaryEntity.getGovernanceMetadata())
        .isStagesExecution(isStagesExecution)
        .stagesExecuted(stageIdentifiers)
        .stagesExecutedNames(stagesExecutedNames)
        .build();
  }

  private Map<String, String> getStageNames(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, List<String> stageIdentifiers) {
    Optional<PipelineEntity> pipelineEntity =
        pipelineService.get(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, false);
    if (pipelineEntity.isPresent()) {
      List<BasicStageInfo> stageInfoList =
          StageExecutionSelectorHelper.getStageInfoList(pipelineEntity.get().getYaml());
      Map<String, String> identifierToNames = new LinkedHashMap<>();
      stageInfoList.forEach(stageInfo -> {
        String identifier = stageInfo.getIdentifier();
        if (stageIdentifiers.contains(identifier)) {
          identifierToNames.put(identifier, stageInfo.getName());
        }
      });
      return identifierToNames;
    }
    throw new InvalidRequestException(
        String.format("Pipeline with the given ID: %s does not exist or has been deleted", pipelineIdentifier));
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
