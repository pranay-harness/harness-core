/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.core.properties;

import io.harness.annotation.RecasterAlias;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("io.harness.yaml.core.properties.NGProperties")
@RecasterAlias("io.harness.yaml.core.properties.NGProperties")
public class NGProperties {
  @ApiModelProperty(dataType = "io.harness.yaml.core.properties.CIProperties") JsonNode ci;
}
