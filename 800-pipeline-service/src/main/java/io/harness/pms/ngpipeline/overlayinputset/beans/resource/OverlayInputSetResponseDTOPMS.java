package io.harness.pms.ngpipeline.overlayinputset.beans.resource;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.EntityGitDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
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
@ApiModel("OverlayInputSetResponse")
public class OverlayInputSetResponseDTOPMS {
  String accountId;
  String orgIdentifier;
  String projectIdentifier;
  String pipelineIdentifier;
  String identifier;
  String name;
  String description;
  List<String> inputSetReferences;
  String overlayInputSetYaml;
  Map<String, String> tags;
  boolean isInvalid;

  @ApiModelProperty(name = "isErrorResponse") boolean isErrorResponse;
  Map<String, String> invalidInputSetReferences;

  @JsonIgnore Long version;

  EntityGitDetails gitDetails;
}
