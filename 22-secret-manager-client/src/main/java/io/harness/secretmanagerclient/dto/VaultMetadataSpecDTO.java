package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("VAULT")
public class VaultMetadataSpecDTO extends SecretManagerMetadataSpecDTO {
  private List<VaultSecretEngineDTO> secretEngines;
}
