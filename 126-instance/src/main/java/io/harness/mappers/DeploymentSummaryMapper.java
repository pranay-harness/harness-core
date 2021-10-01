package io.harness.mappers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.DeploymentSummaryDTO;
import io.harness.entities.DeploymentSummary;
import io.harness.mappers.deploymentinfomapper.DeploymentInfoMapper;

import java.util.ArrayList;
import lombok.experimental.UtilityClass;

@OwnedBy(HarnessTeam.DX)
@UtilityClass
public class DeploymentSummaryMapper {
  public DeploymentSummaryDTO toDTO(DeploymentSummary deploymentSummary) {
    return DeploymentSummaryDTO.builder()
        .accountIdentifier(deploymentSummary.getAccountIdentifier())
        .orgIdentifier(deploymentSummary.getOrgIdentifier())
        .projectIdentifier(deploymentSummary.getProjectIdentifier())
        .pipelineExecutionId(deploymentSummary.getPipelineExecutionId())
        .pipelineExecutionName(deploymentSummary.getPipelineExecutionName())
        .artifactDetails(deploymentSummary.getArtifactDetails())
        .createdAt(deploymentSummary.getCreatedAt())
        .deployedAt(deploymentSummary.getDeployedAt())
        .deployedById(deploymentSummary.getDeployedById())
        .deployedByName(deploymentSummary.getDeployedByName())
        .deploymentInfoDTO(DeploymentInfoMapper.toDTO(deploymentSummary.getDeploymentInfo()))
        .infrastructureMappingId(deploymentSummary.getInfrastructureMappingId())
        .lastModifiedAt(deploymentSummary.getLastModifiedAt())
        .id(deploymentSummary.getId())
        .serverInstanceInfoList(new ArrayList<>())
        .isRollbackDeployment(deploymentSummary.isRollbackDeployment())
        .instanceSyncKey(deploymentSummary.getInstanceSyncKey())
        .build();
  }

  public DeploymentSummary toEntity(DeploymentSummaryDTO deploymentSummaryDTO) {
    return DeploymentSummary.builder()
        .accountIdentifier(deploymentSummaryDTO.getAccountIdentifier())
        .projectIdentifier(deploymentSummaryDTO.getProjectIdentifier())
        .orgIdentifier(deploymentSummaryDTO.getOrgIdentifier())
        .deployedById(deploymentSummaryDTO.getDeployedById())
        .deployedAt(deploymentSummaryDTO.getDeployedAt())
        .deployedByName(deploymentSummaryDTO.getDeployedByName())
        .deploymentInfo(DeploymentInfoMapper.toEntity(deploymentSummaryDTO.getDeploymentInfoDTO()))
        .infrastructureMappingId(deploymentSummaryDTO.getInfrastructureMappingId())
        .pipelineExecutionId(deploymentSummaryDTO.getPipelineExecutionId())
        .pipelineExecutionName(deploymentSummaryDTO.getPipelineExecutionName())
        .artifactDetails(deploymentSummaryDTO.getArtifactDetails())
        .isRollbackDeployment(deploymentSummaryDTO.isRollbackDeployment())
        .instanceSyncKey(deploymentSummaryDTO.getInstanceSyncKey())
        .build();
  }
}
