package io.harness.delegate.beans.azure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.encryption.SecretRefData;
import io.harness.encryption.SecretReference;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("VMAuth")
public class AzureVMAuthDTO implements DecryptableEntity, ExecutionCapabilityDemander {
  private String userName;

  private String secretRefFieldName = "secretRef";
  @ApiModelProperty(dataType = "string") @NotNull @SecretReference SecretRefData secretRef;

  @JsonProperty("type") private AzureVMAuthType azureVmAuthType;

  @Builder
  public AzureVMAuthDTO(SecretRefData secretRef, AzureVMAuthType azureVmAuthType, String userName) {
    this.secretRef = secretRef;
    this.azureVmAuthType = azureVmAuthType;
    this.userName = userName;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return new ArrayList<>();
  }
}
