package io.harness.service.instancesyncperpetualtaskinfo;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfoDTO;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo;
import io.harness.entities.instancesyncperpetualtaskinfo.InstanceSyncPerpetualTaskInfo.InstanceSyncPerpetualTaskInfoKeys;
import io.harness.mappers.DeploymentInfoDetailsMapper;
import io.harness.mappers.InstanceSyncPerpetualTaskInfoMapper;
import io.harness.repositories.instancesyncperpetualtask.InstanceSyncPerpetualTaskRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(DX)
@Singleton
public class InstanceSyncPerpetualTaskInfoServiceImpl implements InstanceSyncPerpetualTaskInfoService {
  @Inject InstanceSyncPerpetualTaskRepository instanceSyncPerpetualTaskRepository;

  @Override
  public Optional<InstanceSyncPerpetualTaskInfoDTO> findByInfrastructureMappingId(String infrastructureMappingId) {
    Optional<InstanceSyncPerpetualTaskInfo> instanceSyncPerpetualTaskInfoOptional =
        instanceSyncPerpetualTaskRepository.findByInfrastructureMappingId(infrastructureMappingId);
    return instanceSyncPerpetualTaskInfoOptional.map(InstanceSyncPerpetualTaskInfoMapper::toDTO);
  }

  @Override
  public Optional<InstanceSyncPerpetualTaskInfoDTO> findByPerpetualTaskId(
      String accountIdentifier, String perpetualTaskId) {
    Optional<InstanceSyncPerpetualTaskInfo> instanceSyncPerpetualTaskInfoOptional =
        instanceSyncPerpetualTaskRepository.findByAccountIdentifierAndPerpetualTaskId(
            accountIdentifier, perpetualTaskId);
    return instanceSyncPerpetualTaskInfoOptional.map(InstanceSyncPerpetualTaskInfoMapper::toDTO);
  }

  @Override
  public InstanceSyncPerpetualTaskInfoDTO save(InstanceSyncPerpetualTaskInfoDTO instanceSyncPerpetualTaskInfoDTO) {
    InstanceSyncPerpetualTaskInfo instanceSyncPerpetualTaskInfo =
        InstanceSyncPerpetualTaskInfoMapper.toEntity(instanceSyncPerpetualTaskInfoDTO);
    instanceSyncPerpetualTaskInfo = instanceSyncPerpetualTaskRepository.save(instanceSyncPerpetualTaskInfo);
    return InstanceSyncPerpetualTaskInfoMapper.toDTO(instanceSyncPerpetualTaskInfo);
  }

  @Override
  public void deleteById(String accountIdentifier, String instanceSyncPerpetualTaskInfoId) {
    instanceSyncPerpetualTaskRepository.deleteByAccountIdentifierAndId(
        accountIdentifier, instanceSyncPerpetualTaskInfoId);
  }

  @Override
  public InstanceSyncPerpetualTaskInfoDTO updateDeploymentInfoDetailsList(
      InstanceSyncPerpetualTaskInfoDTO instanceSyncPerpetualTaskInfoDTO) {
    Criteria criteria = Criteria.where(InstanceSyncPerpetualTaskInfoKeys.accountIdentifier)
                            .is(instanceSyncPerpetualTaskInfoDTO.getAccountIdentifier())
                            .and(InstanceSyncPerpetualTaskInfoKeys.id)
                            .is(instanceSyncPerpetualTaskInfoDTO.getId());
    Update update = new Update().set(InstanceSyncPerpetualTaskInfoKeys.deploymentInfoDetailsList,
        DeploymentInfoDetailsMapper.toDeploymentInfoDetailsEntityList(
            instanceSyncPerpetualTaskInfoDTO.getDeploymentInfoDetailsDTOList()));
    return InstanceSyncPerpetualTaskInfoMapper.toDTO(instanceSyncPerpetualTaskRepository.update(criteria, update));
  }
}
