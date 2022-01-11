/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.pms.ngpipeline.overlayinputset.beans.resource;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.gitsync.sdk.EntityValidityDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(name = "OverlayInputSetResponse", description = "This contains Overlay Input Set details.")
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
  boolean isOutdated;

  @ApiModelProperty(name = "isErrorResponse") boolean isErrorResponse;
  Map<String, String> invalidInputSetReferences;

  @JsonIgnore Long version;

  EntityGitDetails gitDetails;
  EntityValidityDetails entityValidityDetails;
}
