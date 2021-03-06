package io.harness.yaml.core.failurestrategy.manualintervention;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.timeout.Timeout;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManualFailureSpecConfig {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<Timeout> timeout;
  @NotNull OnTimeoutConfig onTimeout;
}
