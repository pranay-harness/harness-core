package io.harness.secretmanagerclient.dto.azurekeyvault;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secretmanagerclient.dto.SecretManagerMetadataSpecDTO;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@OwnedBy(PL)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("AZURE_VAULT")
public class AzureKeyVaultMetadataSpecDTO extends SecretManagerMetadataSpecDTO {
  List<String> vaultNames;
}
