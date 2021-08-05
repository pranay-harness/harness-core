package io.harness.pms.resourceconstraints.response;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("ResourceConstraintExecutionInfo")
@OwnedBy(HarnessTeam.PIPELINE)
public class ResourceConstraintExecutionInfoDTO {
  int capacity;
  String name;

  List<ResourceConstraintDetailDTO> resourceConstraints;
}
