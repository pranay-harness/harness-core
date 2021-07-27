package io.harness.service.instance;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.InstanceDTO;
import io.harness.entities.Instance;
import io.harness.mappers.InstanceMapper;
import io.harness.models.CountByServiceIdAndEnvType;
import io.harness.models.EnvBuildInstanceCount;
import io.harness.models.InstancesByBuildId;
import io.harness.repositories.instance.InstanceRepository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

@Singleton
@OwnedBy(HarnessTeam.DX)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InstanceServiceImpl implements InstanceService {
  private final InstanceRepository instanceRepository;

  @Override
  public InstanceDTO save(InstanceDTO instanceDTO) {
    Instance instance = InstanceMapper.toEntity(instanceDTO);
    instance = instanceRepository.save(instance);
    return InstanceMapper.toDTO(instance);
  }

  /**
   * Create instance record if not present already
   * @param instanceDTO
   * @return  Optional.empty() in case duplicate key issue occurs as record is already present
   *          Instance entity in case record is created successfully
   */
  @Override
  public Optional<InstanceDTO> saveOrReturnEmptyIfAlreadyExists(InstanceDTO instanceDTO) {
    Instance instance = InstanceMapper.toEntity(instanceDTO);
    try {
      instance = instanceRepository.save(instance);
    } catch (DuplicateKeyException duplicateKeyException) {
      log.warn("Duplicate key error while inserting instance : {}", instanceDTO);
      return Optional.empty();
    }
    return Optional.of(InstanceMapper.toDTO(instance));
  }

  @Override
  public List<InstanceDTO> getActiveInstancesByAccount(String accountIdentifier, long timestamp) {
    return InstanceMapper.toDTO(instanceRepository.getActiveInstancesByAccount(accountIdentifier, timestamp));
  }

  @Override
  public List<InstanceDTO> getInstances(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String infrastructureMappingId) {
    return InstanceMapper.toDTO(
        instanceRepository.getInstances(accountIdentifier, orgIdentifier, projectIdentifier, infrastructureMappingId));
  }

  /*
    Returns list of active instances for given account+org+project at given timestamp
  */
  @Override
  public List<InstanceDTO> getActiveInstances(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, long timestampInMs) {
    return InstanceMapper.toDTO(
        instanceRepository.getActiveInstances(accountIdentifier, orgIdentifier, projectIdentifier, timestampInMs));
  }

  /*
    Returns list of active instances for given account+org+project+service at given timestamp
  */
  @Override
  public List<InstanceDTO> getActiveInstancesByServiceId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs) {
    return InstanceMapper.toDTO(instanceRepository.getActiveInstancesByServiceId(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, timestampInMs));
  }

  @Override
  public List<InstanceDTO> getActiveInstancesByInfrastructureMappingId(String accountIdentifier, String orgIdentifier,
      String projectIdentifier, String infrastructureMappingId, long timestampInMs) {
    return InstanceMapper.toDTO(instanceRepository.getActiveInstancesByInfrastructureMappingId(
        accountIdentifier, orgIdentifier, projectIdentifier, infrastructureMappingId, timestampInMs));
  }

  /*
    Returns aggregated result containing unique environment and build ids with instance count
  */
  @Override
  public AggregationResults<EnvBuildInstanceCount> getEnvBuildInstanceCountByServiceId(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String serviceId, long timestampInMs) {
    return instanceRepository.getEnvBuildInstanceCountByServiceId(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, timestampInMs);
  }

  /*
    Returns aggregated result containing total {limit} instances for given buildIds
   */
  @Override
  public AggregationResults<InstancesByBuildId> getActiveInstancesByServiceIdEnvIdAndBuildIds(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, String serviceId, String envId, List<String> buildIds,
      long timestampInMs, int limit) {
    return instanceRepository.getActiveInstancesByServiceIdEnvIdAndBuildIds(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, envId, buildIds, timestampInMs, limit);
  }

  /*
    Returns breakup of active instances by envType at a given timestamp for specified accountIdentifier,
    projectIdentifier, orgIdentifier and serviceIds
  */
  @Override
  public AggregationResults<CountByServiceIdAndEnvType> getActiveServiceInstanceCountBreakdown(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, List<String> serviceId, long timestampInMs) {
    return instanceRepository.getActiveServiceInstanceCountBreakdown(
        accountIdentifier, orgIdentifier, projectIdentifier, serviceId, timestampInMs);
  }
}
