package io.harness.service.deploymentsummary;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.DeploymentSummaryDTO;
import io.harness.entities.DeploymentSummary;
import io.harness.mappers.DeploymentSummaryMapper;
import io.harness.repositories.deploymentsummary.DeploymentSummaryRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.AllArgsConstructor;

@OwnedBy(HarnessTeam.DX)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class DeploymentSummaryServiceImpl implements DeploymentSummaryService {
  private DeploymentSummaryRepository deploymentSummaryRepository;

  @Override
  public DeploymentSummaryDTO save(DeploymentSummaryDTO deploymentSummaryDTO) {
    DeploymentSummary deploymentSummary =
        deploymentSummaryRepository.save(DeploymentSummaryMapper.toEntity(deploymentSummaryDTO));
    return DeploymentSummaryMapper.toDTO(deploymentSummary);
  }

  @Override
  public Optional<DeploymentSummaryDTO> getByDeploymentSummaryId(String deploymentSummaryId) {
    Optional<DeploymentSummary> deploymentSummaryOptional = deploymentSummaryRepository.findById(deploymentSummaryId);
    return deploymentSummaryOptional.map(DeploymentSummaryMapper::toDTO);
  }

  @Override
  public Optional<DeploymentSummaryDTO> getByInfraMappingId(String infraMappingId) {
    Optional<DeploymentSummary> deploymentSummaryOptional =
        deploymentSummaryRepository.getWithInfraMappingId(infraMappingId);
    return deploymentSummaryOptional.map(DeploymentSummaryMapper::toDTO);
  }

  @Override
  public Optional<DeploymentSummaryDTO> getNthDeploymentSummaryFromNow(int N, String instanceSyncKey) {
    Optional<DeploymentSummary> deploymentSummaryOptional =
        deploymentSummaryRepository.fetchNthRecordFromNow(N, instanceSyncKey);
    return deploymentSummaryOptional.map(DeploymentSummaryMapper::toDTO);
  }

  @Override
  public Optional<DeploymentSummaryDTO> getLatestByInstanceKey(String instanceSyncKey) {
    return getNthDeploymentSummaryFromNow(1, instanceSyncKey);
  }
}
