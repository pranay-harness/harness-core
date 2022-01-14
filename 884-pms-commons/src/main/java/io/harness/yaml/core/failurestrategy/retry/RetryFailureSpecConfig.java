package io.harness.yaml.core.failurestrategy.retry;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.timeout.Timeout;

import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(HarnessTeam.PIPELINE)
public class RetryFailureSpecConfig {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.INTEGER_CLASSPATH) ParameterField<Integer> retryCount;
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  @Size(min = 1, message = "should not be empty")
  ParameterField<List<Timeout>> retryIntervals;
  @NotNull OnRetryFailureConfig onRetryFailure;
}
