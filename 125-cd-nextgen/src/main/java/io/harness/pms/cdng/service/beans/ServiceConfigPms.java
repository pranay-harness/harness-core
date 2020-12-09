package io.harness.pms.cdng.service.beans;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.intfc.OverridesApplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Wither;

@Data
@Builder
public class ServiceConfigPms implements OverridesApplier<ServiceConfigPms> {
  String uuid;
  @Wither private ServiceUseFromStagePms useFromStage;

  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) private ParameterField<String> identifier;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> name;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> description;
  private ServiceDefinitionPms serviceDefinition;
  @Wither private StageOverridesConfigPms stageOverrides;
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
