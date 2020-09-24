package io.harness.delegate.beans.azure;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@ToString(exclude = "key")
@EqualsAndHashCode(callSuper = false)
@JsonTypeName("AzureConfig")
public class AzureConfigDTO implements DecryptableEntity, ExecutionCapabilityDemander {
  private static final String AZURE_URL = "https://azure.microsoft.com/";
  private String clientId;
  private String tenantId;

  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData key;

  @Builder
  public AzureConfigDTO(String clientId, String tenantId, SecretRefData key) {
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.key = key;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.singletonList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AZURE_URL));
  }
}
