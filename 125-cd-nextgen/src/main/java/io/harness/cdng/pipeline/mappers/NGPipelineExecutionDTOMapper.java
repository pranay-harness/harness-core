package io.harness.cdng.pipeline.mappers;

import io.harness.cdng.inputset.beans.entities.MergeInputSetResponse;
import io.harness.cdng.pipeline.beans.resources.NGPipelineErrorDTO;
import io.harness.cdng.pipeline.beans.resources.NGPipelineErrorResponseDTO;
import io.harness.cdng.pipeline.beans.resources.NGPipelineErrorWrapperDTO;
import io.harness.cdng.pipeline.beans.resources.NGPipelineExecutionResponseDTO;
import io.harness.data.structure.EmptyPredicate;
import io.harness.execution.PlanExecution;
import io.harness.walktree.visitor.mergeinputset.beans.MergeInputSetErrorResponse;
import io.harness.walktree.visitor.response.VisitorErrorResponseWrapper;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
        .errorPipelineYaml(mergeInputSetResponse.getErrorPipelineYaml())
        .uuidToErrorResponseMap(uuidToErrorResponseMap)
        .build();
  }
}
