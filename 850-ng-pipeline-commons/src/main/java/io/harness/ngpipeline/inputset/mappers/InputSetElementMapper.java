package io.harness.ngpipeline.inputset.mappers;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.ngpipeline.inputset.beans.entities.InputSetEntity;
import io.harness.ngpipeline.inputset.beans.entities.MergeInputSetResponse;
import io.harness.ngpipeline.inputset.beans.resource.InputSetErrorDTO;
import io.harness.ngpipeline.inputset.beans.resource.InputSetErrorResponseDTO;
import io.harness.ngpipeline.inputset.beans.resource.InputSetErrorWrapperDTO;
import io.harness.ngpipeline.inputset.beans.resource.InputSetResponseDTO;
import io.harness.ngpipeline.inputset.beans.resource.InputSetSummaryResponseDTO;
import io.harness.ngpipeline.inputset.beans.resource.MergeInputSetResponseDTO;
import io.harness.ngpipeline.inputset.beans.yaml.InputSetConfig;
import io.harness.ngpipeline.overlayinputset.beans.BaseInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.InputSetEntityType;
import io.harness.ngpipeline.overlayinputset.beans.entities.OverlayInputSetEntity;
import io.harness.ngpipeline.overlayinputset.beans.resource.OverlayInputSetResponseDTO;
import io.harness.overlayinputset.OverlayInputSetConfig;
import io.harness.walktree.visitor.mergeinputset.beans.MergeInputSetErrorResponse;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import io.harness.yaml.utils.JsonPipelineUtils;
import io.harness.yaml.utils.YamlPipelineUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
@ToBeDeleted
@Deprecated
public class InputSetElementMapper {
  public InputSetEntity toInputSetEntity(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml) {
    try {
      InputSetConfig inputSet = YamlPipelineUtils.read(yaml, InputSetConfig.class);
      return toInputSetEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, inputSet);
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot create inputSet entity because: " + e.getMessage());
    }
  }

  public InputSetEntity toInputSetEntityWithIdentifier(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String inputSetIdentifier, String yaml) {
    try {
      InputSetConfig inputSet = YamlPipelineUtils.read(yaml, InputSetConfig.class);
      if (!inputSet.getIdentifier().equals(inputSetIdentifier)) {
        throw new InvalidRequestException("Input set identifier in yaml is invalid");
      }
      return toInputSetEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, inputSet);
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot create inputSet entity because: " + e.getMessage());
    }
  }

  private InputSetEntity toInputSetEntity(String accountId, String orgIdentifier, String projectIdentifier,
      String pipelineIdentifier, String yaml, InputSetConfig inputSet) {
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
    inputSetEntity.setTags(TagMapper.convertToList(inputSet.getTags()));
    return inputSetEntity;
  }

  public OverlayInputSetEntity toOverlayInputSetEntity(
      String accountId, String orgIdentifier, String projectIdentifier, String pipelineIdentifier, String yaml) {
    try {
      OverlayInputSetConfig inputSet = YamlPipelineUtils.read(yaml, OverlayInputSetConfig.class);
      return toOverlayInputSetEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, inputSet);
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot create inputSet entity because: " + e.getMessage());
    }
  }

  public OverlayInputSetEntity toOverlayInputSetEntityWithIdentifier(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String inputSetIdentifier, String yaml) {
    try {
      OverlayInputSetConfig inputSet = YamlPipelineUtils.read(yaml, OverlayInputSetConfig.class);
      if (!inputSet.getIdentifier().equals(inputSetIdentifier)) {
        throw new InvalidRequestException("Input set identifier in yaml is invalid");
      }
      return toOverlayInputSetEntity(accountId, orgIdentifier, projectIdentifier, pipelineIdentifier, yaml, inputSet);
    } catch (Exception e) {
      throw new InvalidRequestException("Cannot create inputSet entity because: " + e.getMessage());
    }
  }

  private OverlayInputSetEntity toOverlayInputSetEntity(String accountId, String orgIdentifier,
      String projectIdentifier, String pipelineIdentifier, String yaml, OverlayInputSetConfig inputSet) {
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
    overlayInputSetEntity.setTags(TagMapper.convertToList(inputSet.getTags()));
    return overlayInputSetEntity;
  }

  public InputSetResponseDTO writeInputSetResponseDTO(
      BaseInputSetEntity inputSetEntity, MergeInputSetResponse mergeResponse) {
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
        .accountId(inputSetEntity.getAccountId())
        .orgIdentifier(inputSetEntity.getOrgIdentifier())
        .projectIdentifier(inputSetEntity.getProjectIdentifier())
        .pipelineIdentifier(inputSetEntity.getPipelineIdentifier())
        .identifier(inputSetEntity.getIdentifier())
        .inputSetYaml(inputSetEntity.getInputSetYaml())
        .name(inputSetEntity.getName())
        .description(inputSetEntity.getDescription())
        .tags(TagMapper.convertToMap(inputSetEntity.getTags()))
        .isErrorResponse(isErrorResponse)
        .inputSetErrorWrapper(inputSetErrorWrapperDTO)
        .version(inputSetEntity.getVersion())
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
        .tags(TagMapper.convertToMap(overlayInputSetEntity.getTags()))
        .inputSetReferences(references)
        .isErrorResponse(isErrorResponse)
        .invalidInputSetReferences(invalidIdentifiers)
        .version(overlayInputSetEntity.getVersion())
        .build();
  }

  public InputSetSummaryResponseDTO writeSummaryResponseDTO(BaseInputSetEntity baseInputSetEntity) {
    return InputSetSummaryResponseDTO.builder()
        .identifier(baseInputSetEntity.getIdentifier())
        .name(baseInputSetEntity.getName())
        .pipelineIdentifier(baseInputSetEntity.getPipelineIdentifier())
        .description(baseInputSetEntity.getDescription())
        .tags(TagMapper.convertToMap(baseInputSetEntity.getTags()))
        .inputSetType(baseInputSetEntity.getInputSetType())
        .version(baseInputSetEntity.getVersion())
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
