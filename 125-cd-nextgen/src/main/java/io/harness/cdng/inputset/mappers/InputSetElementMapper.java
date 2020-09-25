package io.harness.cdng.inputset.mappers;

import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.resource.InputSetResponseDTO;
import io.harness.cdng.inputset.beans.resource.InputSetSummaryResponseDTO;
import io.harness.cdng.inputset.beans.yaml.CDInputSet;
import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.BaseInputSetEntity;
import io.harness.ngpipeline.InputSetEntityType;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTO;
import io.harness.overlayinputset.OverlayInputSet;
import io.harness.yaml.utils.YamlPipelineUtils;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class InputSetElementMapper {
  public CDInputSetEntity toCDInputSetEntity(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml) {
    try {
      CDInputSet inputSet = YamlPipelineUtils.read(yaml, CDInputSet.class);
      CDInputSetEntity cdInputSetEntity = CDInputSetEntity.builder().cdInputSet(inputSet).build();
      cdInputSetEntity.setAccountId(accountId);
      cdInputSetEntity.setOrgIdentifier(orgIdentifier);
      cdInputSetEntity.setProjectIdentifier(projectIdentifier);
      cdInputSetEntity.setPipelineIdentifier(pipelineIdentifier);
      cdInputSetEntity.setIdentifier(inputSet.getIdentifier());
      cdInputSetEntity.setName(inputSet.getName());
      cdInputSetEntity.setDescription(inputSet.getDescription());
      cdInputSetEntity.setInputSetType(InputSetEntityType.INPUT_SET);
      cdInputSetEntity.setInputSetYaml(yaml);
      return cdInputSetEntity;
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
      return toCDInputSetEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot create inputSet entity due to " + e.getMessage());
    }
  }

  public OverlayInputSetEntity toOverlayInputSetEntity(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml) {
    try {
      OverlayInputSet inputSet = YamlPipelineUtils.read(yaml, OverlayInputSet.class);
      OverlayInputSetEntity overlayInputSetEntity =
          OverlayInputSetEntity.builder().inputSetReferences(inputSet.getInputSetReferences()).build();
      overlayInputSetEntity.setAccountId(accountId);
      overlayInputSetEntity.setOrgIdentifier(orgIdentifier);
      overlayInputSetEntity.setProjectIdentifier(projectIdentifier);
      overlayInputSetEntity.setPipelineIdentifier(pipelineIdentifier);
      overlayInputSetEntity.setIdentifier(inputSet.getIdentifier());
      overlayInputSetEntity.setName(inputSet.getName());
      overlayInputSetEntity.setDescription(inputSet.getDescription());
      overlayInputSetEntity.setInputSetType(InputSetEntityType.OVERLAY_INPUT_SET);
      overlayInputSetEntity.setInputSetYaml(yaml);
      return overlayInputSetEntity;
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot create inputSet entity due to " + e.getMessage());
    }
  }

  public OverlayInputSetEntity toOverlayInputSetEntityWithIdentifier(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String inputSetIdentifier, String yaml) {
    try {
      OverlayInputSet inputSet = YamlPipelineUtils.read(yaml, OverlayInputSet.class);
      if (!inputSet.getIdentifier().equals(inputSetIdentifier)) {
        throw new InvalidRequestException("Input set identifier in yaml is invalid");
      }
      return toOverlayInputSetEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot create inputSet entity due to " + e.getMessage());
    }
  }

  public InputSetResponseDTO writeCDInputSetResponseDTO(BaseInputSetEntity cdInputSetEntity) {
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

  public OverlayInputSetResponseDTO writeOverlayResponseDTO(BaseInputSetEntity overlayInputSetEntity) {
    List<String> references = ((OverlayInputSetEntity) overlayInputSetEntity).getInputSetReferences();
    return OverlayInputSetResponseDTO.builder()
        .accountId(overlayInputSetEntity.getAccountId())
        .orgIdentifier(overlayInputSetEntity.getOrgIdentifier())
        .projectIdentifier(overlayInputSetEntity.getProjectIdentifier())
        .pipelineIdentifier(overlayInputSetEntity.getPipelineIdentifier())
        .identifier(overlayInputSetEntity.getIdentifier())
        .overlayInputSetYaml(overlayInputSetEntity.getInputSetYaml())
        .name(overlayInputSetEntity.getName())
        .description(overlayInputSetEntity.getDescription())
        .inputSetReferences(references)
        .build();
  }

  public InputSetSummaryResponseDTO writeSummaryResponseDTO(BaseInputSetEntity baseInputSetEntity) {
    return InputSetSummaryResponseDTO.builder()
        .identifier(baseInputSetEntity.getIdentifier())
        .name(baseInputSetEntity.getName())
        .pipelineIdentifier(baseInputSetEntity.getPipelineIdentifier())
        .description(baseInputSetEntity.getDescription())
        .inputSetType(baseInputSetEntity.getInputSetType())
        .build();
  }
}
