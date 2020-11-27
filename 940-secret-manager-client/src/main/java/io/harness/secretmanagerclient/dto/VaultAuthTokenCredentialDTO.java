package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("TOKEN")
public class VaultAuthTokenCredentialDTO extends VaultCredentialDTO {
  private String authToken;
}
