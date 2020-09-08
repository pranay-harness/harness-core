package io.harness.cdng.inputset.mappers;

import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.resource.InputSetRequestDTO;
import io.harness.cdng.inputset.beans.resource.InputSetResponseDTO;
import io.harness.cdng.inputset.beans.yaml.CDInputSet;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.utils.YamlPipelineUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CDInputSetElementMapper {
  public CDInputSetEntity toCDInputSetEntity(
      String accountId, String orgIdentifier, String projectIdentifier, InputSetRequestDTO inputSetRequestDTO) {
    try {
      return CDInputSetEntity.builder()
          .accountId(accountId)
          .orgIdentifier(orgIdentifier)
          .projectIdentifier(projectIdentifier)
          .pipelineIdentifier(inputSetRequestDTO.getPipelineIdentifier())
          .identifier(inputSetRequestDTO.getIdentifier())
          .cdInputSet(YamlPipelineUtils.read(inputSetRequestDTO.getYaml(), CDInputSet.class))
          .inputSetYaml(inputSetRequestDTO.getYaml())
          .description(inputSetRequestDTO.getDescription())
          .build();
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot create inputSet entity due to " + e.getMessage());
    }
  }

  public InputSetResponseDTO writeResponseDTO(CDInputSetEntity cdInputSetEntity) {
    return InputSetResponseDTO.builder()
        .accountId(cdInputSetEntity.getAccountId())
        .orgIdentifier(cdInputSetEntity.getOrgIdentifier())
        .projectIdentifier(cdInputSetEntity.getProjectIdentifier())
        .pipelineIdentifier(cdInputSetEntity.getPipelineIdentifier())
        .inputSetIdentifier(cdInputSetEntity.getIdentifier())
        .inputSetYaml(cdInputSetEntity.getInputSetYaml())
        .description(cdInputSetEntity.getDescription())
        .build();
  }
}
