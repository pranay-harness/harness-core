/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.pipeline;

import static io.harness.filter.FilterConstants.PIPELINE_SETUP_FILTER;

import io.harness.filter.entity.FilterProperties;
import io.harness.ng.core.common.beans.NGTag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel("PipelineFilterProperties")
@JsonTypeName(PIPELINE_SETUP_FILTER)
public class PipelineFilterProperties extends FilterProperties {
  private List<NGTag> pipelineTags;
  private List<String> pipelineIdentifiers;
  private String name;
  private String description;

  private org.bson.Document moduleProperties;
}
