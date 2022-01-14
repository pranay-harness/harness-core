package io.harness.pms.inputset;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(PIPELINE)
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("MergeInputSetResponse")
@Schema(name = "MergeInputSetResponse", description = "View of the Response of Merging of Input Sets of a Pipeline")
public class MergeInputSetResponseDTOPMS {
  String pipelineYaml;
  String completePipelineYaml;

  @ApiModelProperty(name = "isErrorResponse") boolean isErrorResponse;
  InputSetErrorWrapperDTOPMS inputSetErrorWrapper;
}
