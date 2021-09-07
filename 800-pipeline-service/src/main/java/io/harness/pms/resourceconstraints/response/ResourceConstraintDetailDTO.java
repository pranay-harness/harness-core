package io.harness.pms.resourceconstraints.response;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.distribution.constraint.Consumer.State;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("ResourceConstraintDetail")
@OwnedBy(HarnessTeam.PIPELINE)
public class ResourceConstraintDetailDTO {
  String pipelineIdentifier;
  String planExecutionId;
  State state;
}
