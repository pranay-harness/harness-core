package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("VAULT")
public class VaultMetadataSpecDTO extends SecretManagerMetadataSpecDTO {
  private List<VaultSecretEngineDTO> secretEngines;
}
