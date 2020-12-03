package io.harness.delegate.beans.connector.splunkconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.harness.expression.ExpressionEvaluator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import java.util.Arrays;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SplunkConnectorDTO extends ConnectorConfigDTO implements DecryptableEntity, ExecutionCapabilityDemander {
  String splunkUrl;
  String username;
  @NotNull String accountId;
  public String getSplunkUrl() {
    if (splunkUrl.endsWith("/")) {
      return splunkUrl;
    }
    return splunkUrl + "/";
  }
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData passwordRef;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        getSplunkUrl(), maskingEvaluator));
  }

  @Override
  public DecryptableEntity getDecryptableEntity() {
    return this;
  }
}
