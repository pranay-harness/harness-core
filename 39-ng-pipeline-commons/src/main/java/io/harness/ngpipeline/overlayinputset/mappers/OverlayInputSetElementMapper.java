package io.harness.ngpipeline.overlayinputset.mappers;

import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTO;
import io.harness.overlayinputset.OverlayInputSet;
import io.harness.yaml.utils.YamlPipelineUtils;
import lombok.experimental.UtilityClass;

@UtilityClass
public class OverlayInputSetElementMapper {
  public OverlayInputSetEntity toOverlayInputSetEntity(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml) {
    try {
      OverlayInputSet inputSet = YamlPipelineUtils.read(yaml, OverlayInputSet.class);
      return OverlayInputSetEntity.builder()
          .accountId(accountId)
          .orgIdentifier(orgIdentifier)
          .projectIdentifier(projectIdentifier)
          .pipelineIdentifier(pipelineIdentifier)
          .identifier(inputSet.getIdentifier())
          .overlayInputSetYaml(yaml)
          .name(inputSet.getName())
          .description(inputSet.getDescription())
          .inputSetsReferenceList(inputSet.getInputSetsReferences())
          .build();
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
      return OverlayInputSetEntity.builder()
          .accountId(accountId)
          .orgIdentifier(orgIdentifier)
          .projectIdentifier(projectIdentifier)
          .pipelineIdentifier(pipelineIdentifier)
          .identifier(inputSet.getIdentifier())
          .overlayInputSetYaml(yaml)
          .name(inputSet.getName())
          .description(inputSet.getDescription())
          .inputSetsReferenceList(inputSet.getInputSetsReferences())
          .build();
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot create inputSet entity due to " + e.getMessage());
    }
  }

  public OverlayInputSetResponseDTO writeResponseDTO(OverlayInputSetEntity overlayInputSetEntity) {
    return OverlayInputSetResponseDTO.builder()
        .accountId(overlayInputSetEntity.getAccountId())
        .orgIdentifier(overlayInputSetEntity.getOrgIdentifier())
        .projectIdentifier(overlayInputSetEntity.getProjectIdentifier())
        .pipelineIdentifier(overlayInputSetEntity.getPipelineIdentifier())
        .identifier(overlayInputSetEntity.getIdentifier())
        .overlayInputSetYaml(overlayInputSetEntity.getOverlayInputSetYaml())
        .name(overlayInputSetEntity.getName())
        .description(overlayInputSetEntity.getDescription())
        .inputSetReferences(overlayInputSetEntity.getInputSetsReferenceList())
        .build();
  }
}
