package io.harness.delegate.beans.connector.appdynamicsconnector;

import static io.harness.yamlSchema.NGSecretReferenceConstants.SECRET_REF_PATTERN;

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
import javax.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppDynamicsConnectorDTO
    extends ConnectorConfigDTO implements DecryptableEntity, ExecutionCapabilityDemander {
  @NotNull String username;
  @NotNull String accountname;
  @NotNull String controllerUrl;
  @NotNull String accountId;

  @ApiModelProperty(dataType = "string")
  @NotNull
  @SecretReference
  @Pattern(regexp = SECRET_REF_PATTERN)
  SecretRefData passwordRef;

  public String getControllerUrl() {
    if (controllerUrl.endsWith("/")) {
      return controllerUrl;
    }
    return controllerUrl + "/";
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      io.harness.expression.ExpressionEvaluator maskingEvaluator) {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        getControllerUrl(), maskingEvaluator));
  }

  @Override
  public DecryptableEntity getDecryptableEntity() {
    return this;
  }
}
