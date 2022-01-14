package io.harness.helpers.ext.vault;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@OwnedBy(PL)
public class SSHVaultAuthResponse {
  private boolean renewable;
  @JsonProperty("data") private SSHVaultAuthResult sshVaultAuthResult;
}