package io.harness.cdng.inputset.mappers;

import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.resource.InputSetResponseDTO;
import io.harness.cdng.inputset.beans.resource.InputSetSummaryResponseDTO;
import io.harness.cdng.inputset.beans.yaml.CDInputSet;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.utils.YamlPipelineUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CDInputSetElementMapper {
  public CDInputSetEntity toCDInputSetEntity(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml) {
    try {
      CDInputSet inputSet = YamlPipelineUtils.read(yaml, CDInputSet.class);
      return CDInputSetEntity.builder()
          .accountId(accountId)
          .orgIdentifier(orgIdentifier)
          .projectIdentifier(projectIdentifier)
          .pipelineIdentifier(pipelineIdentifier)
          .identifier(inputSet.getIdentifier())
          .cdInputSet(inputSet)
          .inputSetYaml(yaml)
          .name(inputSet.getName())
          .description(inputSet.getDescription())
          .build();
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot create inputSet entity due to " + e.getMessage());
    }
  }

  public CDInputSetEntity toCDInputSetEntityWithIdentifier(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String inputSetIdentifier, String yaml) {
    try {
      CDInputSet inputSet = YamlPipelineUtils.read(yaml, CDInputSet.class);
      if (!inputSet.getIdentifier().equals(inputSetIdentifier)) {
        throw new InvalidRequestException("Input set identifier in yaml is invalid");
      }
      return CDInputSetEntity.builder()
          .accountId(accountId)
          .orgIdentifier(orgIdentifier)
          .projectIdentifier(projectIdentifier)
          .pipelineIdentifier(pipelineIdentifier)
          .identifier(inputSet.getIdentifier())
          .cdInputSet(inputSet)
          .inputSetYaml(yaml)
          .name(inputSet.getName())
          .description(inputSet.getDescription())
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
        .identifier(cdInputSetEntity.getIdentifier())
        .inputSetYaml(cdInputSetEntity.getInputSetYaml())
        .name(cdInputSetEntity.getName())
        .description(cdInputSetEntity.getDescription())
        .build();
  }

  public InputSetSummaryResponseDTO writeSummaryResponseDTO(CDInputSetEntity cdInputSetEntity) {
    return InputSetSummaryResponseDTO.builder()
        .identifier(cdInputSetEntity.getIdentifier())
        .name(cdInputSetEntity.getName())
        .pipelineIdentifier(cdInputSetEntity.getPipelineIdentifier())
        .description(cdInputSetEntity.getDescription())
        .isOverlaySet(false)
        .build();
  }
}
