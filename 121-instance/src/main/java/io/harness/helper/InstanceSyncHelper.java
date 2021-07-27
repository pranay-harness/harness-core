package io.harness.helper;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.dtos.InfrastructureMappingDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.service.entity.ServiceEntity;
import io.harness.ng.core.service.services.ServiceEntityService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import lombok.AllArgsConstructor;

@OwnedBy(HarnessTeam.DX)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class InstanceSyncHelper {
  private final ServiceEntityService serviceEntityService;
  private final EnvironmentService environmentService;

  public ServiceEntity fetchService(InfrastructureMappingDTO infrastructureMappingDTO) {
    Optional<ServiceEntity> serviceEntityOptional = serviceEntityService.get(
        infrastructureMappingDTO.getAccountIdentifier(), infrastructureMappingDTO.getOrgIdentifier(),
        infrastructureMappingDTO.getProjectIdentifier(), infrastructureMappingDTO.getServiceIdentifier(), false);
    return serviceEntityOptional.orElseThrow(()
                                                 -> new InvalidRequestException("Service not found for serviceId : {}"
                                                     + infrastructureMappingDTO.getServiceIdentifier()));
  }

  public Environment fetchEnvironment(InfrastructureMappingDTO infrastructureMappingDTO) {
    Optional<Environment> environmentServiceOptional = environmentService.get(
        infrastructureMappingDTO.getAccountIdentifier(), infrastructureMappingDTO.getOrgIdentifier(),
        infrastructureMappingDTO.getProjectIdentifier(), infrastructureMappingDTO.getEnvIdentifier(), false);
    return environmentServiceOptional.orElseThrow(
        ()
            -> new InvalidRequestException(
                "Environment not found for envId : {}" + infrastructureMappingDTO.getEnvIdentifier()));
  }
}
