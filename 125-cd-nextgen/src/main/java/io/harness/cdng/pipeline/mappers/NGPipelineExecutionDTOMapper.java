package io.harness.cdng.pipeline.mappers;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.PlanExecution;
import io.harness.ngpipeline.inputset.beans.entities.MergeInputSetResponse;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineErrorDTO;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineErrorResponseDTO;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineErrorWrapperDTO;
import io.harness.ngpipeline.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.walktree.visitor.mergeinputset.beans.MergeInputSetErrorResponse;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import io.harness.yaml.utils.JsonPipelineUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NGPipelineExecutionDTOMapper {
  public NGPipelineExecutionResponseDTO toNGPipelineResponseDTO(
      PlanExecution planExecution, MergeInputSetResponse mergeInputSetResponse) {
    return NGPipelineExecutionResponseDTO.builder()
        .planExecution(planExecution)
        .isErrorResponse(mergeInputSetResponse.isErrorResponse())
        .pipelineErrorResponse(toPipelineErrorWrapperDTO(mergeInputSetResponse))
        .build();
  }

  public NGPipelineErrorWrapperDTO toPipelineErrorWrapperDTO(MergeInputSetResponse mergeInputSetResponse) {
    if (!mergeInputSetResponse.isErrorResponse()) {
      return NGPipelineErrorWrapperDTO.builder().build();
    }
    String errorPipelineResponse;
    try {
      errorPipelineResponse =
          JsonPipelineUtils.writeYamlString(mergeInputSetResponse.getErrorPipeline()).replaceAll("---\n", "");
    } catch (IOException e) {
      throw new InvalidRequestException("Pipeline could not be converted to yaml.");
    }
    Map<String, NGPipelineErrorResponseDTO> uuidToErrorResponseMap = new HashMap<>();

    if (EmptyPredicate.isNotEmpty(mergeInputSetResponse.getUuidToErrorResponseMap())) {
      for (Map.Entry<String, VisitorErrorResponseWrapper> entry :
          mergeInputSetResponse.getUuidToErrorResponseMap().entrySet()) {
        List<NGPipelineErrorDTO> errorDTOS = new LinkedList<>();
        entry.getValue().getErrors().forEach(error -> {
          MergeInputSetErrorResponse errorResponse = (MergeInputSetErrorResponse) error;
          errorDTOS.add(NGPipelineErrorDTO.builder()
                            .fieldName(error.getFieldName())
                            .message(error.getMessage())
                            .identifierOfErrorSource(errorResponse.getIdentifierOfErrorSource())
                            .build());
        });

        uuidToErrorResponseMap.put(entry.getKey(), NGPipelineErrorResponseDTO.builder().errors(errorDTOS).build());
      }
    }
    return NGPipelineErrorWrapperDTO.builder()
        .errorPipelineYaml(errorPipelineResponse)
        .uuidToErrorResponseMap(uuidToErrorResponseMap)
        .build();
  }
}
