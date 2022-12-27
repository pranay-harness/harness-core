/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cdng.aws.asg;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.yaml.schema.beans.SupportedPossibleFieldTypes.runtime;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.plancreator.steps.TaskSelectorYaml;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.YamlSchemaTypes;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@AllArgsConstructor
@TypeAlias("AsgRollingDeployBaseStepInfo")
public class AsgRollingDeployBaseStepInfo {
  @YamlSchemaTypes({runtime})
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<TaskSelectorYaml>> delegateSelectors;

  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH)
  @JsonProperty("skipMatching")
  ParameterField<Boolean> skipMatching;

  @ApiModelProperty(dataType = SwaggerConstants.BOOLEAN_CLASSPATH)
  @JsonProperty("useAlreadyRunningInstances")
  ParameterField<Boolean> useAlreadyRunningInstances;

  @ApiModelProperty(dataType = SwaggerConstants.INTEGER_CLASSPATH)
  @JsonProperty("instanceWarmup")
  ParameterField<Integer> instanceWarmup;

  @ApiModelProperty(dataType = SwaggerConstants.DOUBLE_CLASSPATH)
  @JsonProperty("minimumHealthyPercentage")
  ParameterField<Double> minimumHealthyPercentage;
}