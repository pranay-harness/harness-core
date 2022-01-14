package io.harness.secretmanagerclient.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VaultSecretEngineDTO {
  private String name;
  private String description;
  private String type;
  private Integer version;
}
