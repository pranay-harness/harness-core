package io.harness.secretmanagerclient.dto.azurekeyvault;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.azure.AzureEnvironmentType.AZURE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.azure.AzureEnvironmentType;
import io.harness.encryption.SecretRefData;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataRequestSpecDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
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
  @ApiModelProperty(dataType = "string") @NotNull private SecretRefData secretKey;
  @NotNull private String subscription;
  private AzureEnvironmentType azureEnvironmentType = AZURE;
  private Set<String> delegateSelectors;
}
