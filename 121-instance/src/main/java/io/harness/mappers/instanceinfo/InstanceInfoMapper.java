package io.harness.mappers.instanceinfo;

import io.harness.dtos.instanceinfo.InstanceInfoDTO;
import io.harness.dtos.instanceinfo.ReferenceInstanceInfoDTO;
import io.harness.entities.instanceinfo.InstanceInfo;
import io.harness.entities.instanceinfo.ReferenceInstanceInfo;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@io.harness.annotations.dev.OwnedBy(io.harness.annotations.dev.HarnessTeam.DX)
@UtilityClass
public class InstanceInfoMapper {
  public InstanceInfoDTO toDTO(InstanceInfo instanceInfo) {
    if (instanceInfo instanceof ReferenceInstanceInfo) {
      return ReferenceInstanceInfoMapper.toDTO((ReferenceInstanceInfo) instanceInfo);
    }
    throw new InvalidRequestException("No InstanceInfoMapper toDTO found for instanceInfo : {}" + instanceInfo);
  }

  public InstanceInfo toEntity(InstanceInfoDTO instanceInfoDTO) {
    if (instanceInfoDTO instanceof ReferenceInstanceInfoDTO) {
      return ReferenceInstanceInfoMapper.toEntity((ReferenceInstanceInfoDTO) instanceInfoDTO);
    }
    throw new InvalidRequestException(
        "No InstanceInfoMapper toEntity found for instanceInfoDTO : {}" + instanceInfoDTO);
  }
}
