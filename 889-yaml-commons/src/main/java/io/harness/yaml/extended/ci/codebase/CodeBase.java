/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.extended.ci.codebase;

import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.string;

import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;
import io.harness.yaml.extended.ci.container.ContainerResource;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@TypeAlias("io.harness.yaml.extended.ci.CodeBase")
public class CodeBase {
  @NotNull String connectorRef;
  String repoName;
  @YamlSchemaTypes(value = {string})
  @ApiModelProperty(dataType = "io.harness.yaml.extended.ci.codebase.Build")
  @NotNull
  ParameterField<Build> build;
  Integer depth;
  Boolean sslVerify;
  PRCloneStrategy prCloneStrategy;
  ContainerResource resources;
}
