package io.harness.azure.model.blueprint.vault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretValueReference {
  private KeyVaultReference keyVault;
  private String secretName;
  private String secretVersion;
}
