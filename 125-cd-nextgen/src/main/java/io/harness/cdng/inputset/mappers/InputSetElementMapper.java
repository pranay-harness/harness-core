package io.harness.cdng.inputset.mappers;

import io.harness.cdng.inputset.beans.entities.CDInputSetEntity;
import io.harness.cdng.inputset.beans.entities.MergeInputSetResponse;
import io.harness.cdng.inputset.beans.resource.InputSetErrorDTO;
import io.harness.cdng.inputset.beans.resource.InputSetErrorResponseDTO;
import io.harness.cdng.inputset.beans.resource.InputSetErrorWrapperDTO;
import io.harness.cdng.inputset.beans.resource.InputSetResponseDTO;
import io.harness.cdng.inputset.beans.resource.InputSetSummaryResponseDTO;
import io.harness.cdng.inputset.beans.resource.MergeInputSetResponseDTO;
import io.harness.cdng.inputset.beans.yaml.CDInputSet;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.InputSetEntityType;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTO;
import io.harness.overlayinputset.OverlayInputSet;
import io.harness.walktree.visitor.mergeinputset.beans.MergeInputSetErrorResponse;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import io.harness.yaml.utils.JsonPipelineUtils;
import io.harness.yaml.utils.YamlPipelineUtils;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

  public MergeInputSetResponseDTO toMergeInputSetResponseDTO(MergeInputSetResponse mergeInputSetResponse) {
    try {
      String pipelineResponse = "";
      if (!mergeInputSetResponse.isErrorResponse()) {
        pipelineResponse =
            JsonPipelineUtils.writeYamlString(mergeInputSetResponse.getMergedPipeline()).replaceAll("---\n", "");
      }
      return MergeInputSetResponseDTO.builder()
          .pipelineYaml(pipelineResponse)
          .isErrorResponse(mergeInputSetResponse.isErrorResponse())
          .inputSetErrorWrapper(toInputSetErrorWrapperDTO(mergeInputSetResponse))
          .build();
    } catch (IOException e) {
      throw new InvalidRequestException("Pipeline could not be converted to yaml.");
    }
  }

  private InputSetErrorWrapperDTO toInputSetErrorWrapperDTO(MergeInputSetResponse mergeInputSetResponse) {
    if (!mergeInputSetResponse.isErrorResponse()) {
      return InputSetErrorWrapperDTO.builder().build();
    }

    String errorPipelineResponse;
    try {
      errorPipelineResponse =
          JsonPipelineUtils.writeYamlString(mergeInputSetResponse.getErrorPipeline()).replaceAll("---\n", "");
    } catch (IOException e) {
      throw new InvalidRequestException("Pipeline could not be converted to yaml.");
    }

    Map<String, InputSetErrorResponseDTO> uuidToErrorResponseMap = new HashMap<>();

    if (EmptyPredicate.isNotEmpty(mergeInputSetResponse.getUuidToErrorResponseMap())) {
      for (Map.Entry<String, VisitorErrorResponseWrapper> entry :
          mergeInputSetResponse.getUuidToErrorResponseMap().entrySet()) {
        List<InputSetErrorDTO> errorDTOS = new LinkedList<>();
        entry.getValue().getErrors().forEach(error -> {
          MergeInputSetErrorResponse errorResponse = (MergeInputSetErrorResponse) error;
          errorDTOS.add(InputSetErrorDTO.builder()
                            .fieldName(error.getFieldName())
                            .message(error.getMessage())
                            .identifierOfErrorSource(errorResponse.getIdentifierOfErrorSource())
                            .build());
        });

        uuidToErrorResponseMap.put(entry.getKey(), InputSetErrorResponseDTO.builder().errors(errorDTOS).build());
      }
    }

    return InputSetErrorWrapperDTO.builder()
        .errorPipelineYaml(errorPipelineResponse)
        .uuidToErrorResponseMap(uuidToErrorResponseMap)
        .build();
  }
}
