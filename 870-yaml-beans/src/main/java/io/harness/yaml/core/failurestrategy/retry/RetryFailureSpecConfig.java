package io.harness.yaml.core.failurestrategy.retry;

import io.harness.common.SwaggerConstants;
import io.harness.yaml.core.timeout.Timeout;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RetryFailureSpecConfig {
  @NotNull int retryCount;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH) List<Timeout> retryIntervals;
  @NotNull OnRetryFailureConfig onRetryFailure;
}
