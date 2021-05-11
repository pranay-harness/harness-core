package io.harness.pms.ngpipeline.inputset.beans.resource;

import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.pms.ngpipeline.inputset.beans.entity.InputSetEntityType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ApiModel("InputSetSummaryResponse")
public class InputSetSummaryResponseDTOPMS {
  String identifier;
  String name;
  String pipelineIdentifier;
  String description;
  InputSetEntityType inputSetType;
  Map<String, String> tags;
  @JsonIgnore Long version;
  EntityGitDetails gitDetails;
}
