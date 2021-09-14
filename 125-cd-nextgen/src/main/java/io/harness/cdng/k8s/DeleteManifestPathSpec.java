/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.delegate.task.k8s.DeleteResourcesType;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;

@OwnedBy(CDP)
@Data
@JsonTypeName("ManifestPath")
@RecasterAlias("io.harness.cdng.k8s.DeleteManifestPathSpec")
public class DeleteManifestPathSpec implements DeleteResourcesBaseSpec {
  @YamlSchemaTypes({runtime})
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<String>> manifestPaths;
  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH) ParameterField<Boolean> allManifestPaths;

  @Override
  public DeleteResourcesType getType() {
    return DeleteResourcesType.ManifestPath;
  }

  @Override
  public String getManifestPathsValue() {
    List<String> filePathsList = manifestPaths != null ? manifestPaths.getValue() : Collections.emptyList();
    return filePathsList.stream().collect(Collectors.joining(","));
  }

  @Override
  public String getResourceNamesValue() {
    return "";
  }

  @Override
  public Boolean getAllManifestPathsValue() {
    return allManifestPaths != null && allManifestPaths.getValue() != null && allManifestPaths.getValue();
  }
}
