package io.harness.template.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.template.TemplateInputsErrorResponseDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("TemplateWrapperResponse")
@Schema(name = "TemplateWrapperResponse", description = "This contains details of the Template Wrapper Response")
public class TemplateWrapperResponseDTO {
  boolean isValid;
  TemplateInputsErrorResponseDTO templateInputsErrorResponseDTO;
  TemplateResponseDTO templateResponseDTO;
}
