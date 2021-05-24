package io.harness.secretmanagerclient.dto.azurekeyvault;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.azure.AzureEnvironmentType.AZURE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.AzureEnvironmentType;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestSpecDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("AZURE_VAULT")
public class AzureKeyVaultMetadataRequestSpecDTO extends SecretManagerMetadataRequestSpecDTO {
  @NotNull private String clientId;
  @NotNull private String tenantId;
  @NotNull private String secretKey;
  @NotNull private String subscription;
  private AzureEnvironmentType azureEnvironmentType = AZURE;
}
