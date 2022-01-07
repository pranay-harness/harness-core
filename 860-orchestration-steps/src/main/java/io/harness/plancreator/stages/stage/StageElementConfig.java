/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.plancreator.stages.stage;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.template.yaml.TemplateLinkConfig;
import io.harness.validation.OneOfSet;
import io.harness.validator.NGRegexValidatorConstants;
import io.harness.when.beans.StageWhenCondition;
import io.harness.yaml.core.failurestrategy.FailureStrategyConfig;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PIPELINE)
@Data
@NoArgsConstructor
@TypeAlias("stageElementConfig")
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OneOfSet(fields = {"skipCondition, when, failureStrategies, type, stageType, variables, tags", "template"},
    requiredFieldNames = {"type", "template"})
public class StageElementConfig {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  String uuid;

  @NotNull @EntityIdentifier @Pattern(regexp = NGRegexValidatorConstants.IDENTIFIER_PATTERN) String identifier;
  @NotNull @EntityName @Pattern(regexp = NGRegexValidatorConstants.NAME_PATTERN) String name;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> description;

  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  ParameterField<String> skipCondition;

  StageWhenCondition when;

  List<FailureStrategyConfig> failureStrategies;
  List<NGVariable> variables;
  Map<String, String> tags;
  String type;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  StageInfoConfig stageType;
  TemplateLinkConfig template;

  @Builder
  public StageElementConfig(String uuid, String identifier, String name, ParameterField<String> description,
      List<FailureStrategyConfig> failureStrategies, List<NGVariable> variables, String type, StageInfoConfig stageType,
      ParameterField<String> skipCondition, StageWhenCondition when) {
    this.uuid = uuid;
    this.identifier = identifier;
    this.name = name;
    this.description = description;
    this.failureStrategies = failureStrategies;
    this.variables = variables;
    this.type = type;
    this.stageType = stageType;
    this.skipCondition = skipCondition;
    this.when = when;
  }
}
