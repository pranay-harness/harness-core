package io.harness.pms.cdng.service.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.pms.yaml.YamlNode;
import io.harness.yaml.core.intfc.OverridesApplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceConfigPms implements OverridesApplier<ServiceConfigPms> {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  String uuid;

  @Wither ServiceUseFromStagePms useFromStage;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> identifier;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> name;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither ParameterField<String> description;
  ServiceDefinitionPms serviceDefinition;
  @Wither StageOverridesConfigPms stageOverrides;
  @Wither Map<String, String> tags;

  @JsonIgnore
  public ServiceConfigPms applyUseFromStage(ServiceConfigPms serviceConfigToUseFrom) {
    return serviceConfigToUseFrom.withStageOverrides(stageOverrides).withUseFromStage(useFromStage);
  }

  @Override
  public ServiceConfigPms applyOverrides(ServiceConfigPms overrideConfig) {
    ServiceConfigPms resultantConfig = this;
    if (overrideConfig.getName() != null) {
      resultantConfig = resultantConfig.withName(overrideConfig.getName());
    }
    if (overrideConfig.getDescription() != null) {
      resultantConfig = resultantConfig.withDescription(overrideConfig.getDescription());
    }
    return resultantConfig;
  }
}
