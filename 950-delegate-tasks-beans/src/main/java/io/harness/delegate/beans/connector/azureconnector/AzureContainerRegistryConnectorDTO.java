package io.harness.delegate.beans.connector.azureconnector;

import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
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
public class AzureContainerRegistryConnectorDTO extends ConnectorConfigDTO implements ExecutionCapabilityDemander {
  @NotNull String subscriptionId;
  @NotNull String resourceGroupName;
  @NotNull String azureRegistryName;
  @NotNull String azureRegistryLoginServer;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return Collections.singletonList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        azureRegistryLoginServer.endsWith("/") ? azureRegistryLoginServer : azureRegistryLoginServer.concat("/"),
        maskingEvaluator));
  }

  @Override
  public DecryptableEntity getDecryptableEntity() {
    return null;
  }
}
