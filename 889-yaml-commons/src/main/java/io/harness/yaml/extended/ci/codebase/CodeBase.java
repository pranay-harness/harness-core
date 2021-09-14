/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
