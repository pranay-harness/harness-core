package io.harness.yaml.core.failurestrategy.retry;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.timeout.Timeout;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RetryFailureSpecConfig {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.INTEGER_CLASSPATH) ParameterField<Integer> retryCount;
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  ParameterField<List<Timeout>> retryIntervals;
  @NotNull OnRetryFailureConfig onRetryFailure;
}
