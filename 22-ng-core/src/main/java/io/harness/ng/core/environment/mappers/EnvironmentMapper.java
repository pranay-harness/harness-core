package io.harness.ng.core.environment.mappers;

import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.dto.EnvironmentRequestDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EnvironmentMapper {
  public Environment toEnvironmentEntity(String accountId, EnvironmentRequestDTO environmentRequestDTO) {
    return Environment.builder()
        .identifier(environmentRequestDTO.getIdentifier())
        .accountId(accountId)
        .orgIdentifier(environmentRequestDTO.getOrgIdentifier())
        .projectIdentifier(environmentRequestDTO.getProjectIdentifier())
        .name(environmentRequestDTO.getName())
        .type(environmentRequestDTO.getType())
        .build();
  }

  public EnvironmentResponseDTO writeDTO(Environment environment) {
    return EnvironmentResponseDTO.builder()
        .accountId(environment.getAccountId())
        .orgIdentifier(environment.getOrgIdentifier())
        .projectIdentifier(environment.getProjectIdentifier())
        .identifier(environment.getIdentifier())
        .name(environment.getName())
        .type(environment.getType())
        .deleted(environment.getDeleted())
        .build();
  }
}
