package io.harness.ngpipeline.inputset.mappers;

import io.harness.ngpipeline.inputset.beans.entities.InputSetEntity;
import io.harness.ngpipeline.inputset.beans.entities.MergeInputSetResponse;
import io.harness.ngpipeline.inputset.beans.resource.InputSetErrorDTO;
import io.harness.ngpipeline.inputset.beans.resource.InputSetErrorResponseDTO;
import io.harness.ngpipeline.inputset.beans.resource.InputSetErrorWrapperDTO;
import io.harness.ngpipeline.inputset.beans.resource.InputSetResponseDTO;
import io.harness.ngpipeline.inputset.beans.resource.InputSetSummaryResponseDTO;
import io.harness.ngpipeline.inputset.beans.resource.MergeInputSetResponseDTO;
import io.harness.ngpipeline.inputset.beans.yaml.InputSetConfig;
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
  public InputSetEntity toInputSetEntity(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml) {
    try {
      InputSetConfig inputSet = YamlPipelineUtils.read(yaml, InputSetConfig.class);
      InputSetEntity inputSetEntity = InputSetEntity.builder().inputSetConfig(inputSet).build();
      inputSetEntity.setAccountId(accountId);
      inputSetEntity.setOrgIdentifier(orgIdentifier);
      inputSetEntity.setProjectIdentifier(projectIdentifier);
      inputSetEntity.setPipelineIdentifier(pipelineIdentifier);
      inputSetEntity.setIdentifier(inputSet.getIdentifier());
      inputSetEntity.setName(inputSet.getName());
      inputSetEntity.setDescription(inputSet.getDescription());
      inputSetEntity.setInputSetType(InputSetEntityType.INPUT_SET);
      inputSetEntity.setInputSetYaml(yaml);
      return inputSetEntity;
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot create inputSet entity due to " + e.getMessage());
    }
  }

  public InputSetEntity toInputSetEntityWithIdentifier(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String inputSetIdentifier, String yaml) {
    try {
      InputSetConfig inputSet = YamlPipelineUtils.read(yaml, InputSetConfig.class);
      if (!inputSet.getIdentifier().equals(inputSetIdentifier)) {
        throw new InvalidRequestException("Input set identifier in yaml is invalid");
      }
      return toInputSetEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml);
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

  public InputSetResponseDTO writeInputSetResponseDTO(
      BaseInputSetEntity cdInputSetEntity, MergeInputSetResponse mergeResponse) {
    InputSetErrorWrapperDTO inputSetErrorWrapperDTO;
    boolean isErrorResponse;
    if (mergeResponse == null) {
      inputSetErrorWrapperDTO = null;
      isErrorResponse = false;
    } else {
      inputSetErrorWrapperDTO = toInputSetErrorWrapperDTO(mergeResponse);
      isErrorResponse = true;
    }
    return InputSetResponseDTO.builder()
        .accountId(cdInputSetEntity.getAccountId())
        .orgIdentifier(cdInputSetEntity.getOrgIdentifier())
        .projectIdentifier(cdInputSetEntity.getProjectIdentifier())
        .pipelineIdentifier(cdInputSetEntity.getPipelineIdentifier())
        .identifier(cdInputSetEntity.getIdentifier())
        .inputSetYaml(cdInputSetEntity.getInputSetYaml())
        .name(cdInputSetEntity.getName())
        .description(cdInputSetEntity.getDescription())
        .isErrorResponse(isErrorResponse)
        .inputSetErrorWrapper(inputSetErrorWrapperDTO)
        .build();
  }

  public OverlayInputSetResponseDTO writeOverlayResponseDTO(
      BaseInputSetEntity overlayInputSetEntity, Map<String, String> invalidIdentifiers) {
    List<String> references = ((OverlayInputSetEntity) overlayInputSetEntity).getInputSetReferences();
    boolean isErrorResponse = EmptyPredicate.isNotEmpty(invalidIdentifiers);
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
        .isErrorResponse(isErrorResponse)
        .invalidInputSetReferences(invalidIdentifiers)
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
