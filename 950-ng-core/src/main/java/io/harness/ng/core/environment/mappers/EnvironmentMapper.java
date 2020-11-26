package io.harness.ng.core.environment.mappers;

import static io.harness.ng.core.mapper.TagMapper.convertToList;
import static io.harness.ng.core.mapper.TagMapper.convertToMap;

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
        .description(environmentRequestDTO.getDescription())
        .type(environmentRequestDTO.getType())
        .tags(convertToList(environmentRequestDTO.getTags()))
        .version(environmentRequestDTO.getVersion())
        .build();
  }

  public EnvironmentResponseDTO writeDTO(Environment environment) {
    return EnvironmentResponseDTO.builder()
        .accountId(environment.getAccountId())
        .orgIdentifier(environment.getOrgIdentifier())
        .projectIdentifier(environment.getProjectIdentifier())
        .identifier(environment.getIdentifier())
        .name(environment.getName())
        .description(environment.getDescription())
        .type(environment.getType())
        .deleted(environment.getDeleted())
        .tags(convertToMap(environment.getTags()))
        .version(environment.getVersion())
        .build();
  }
}
