package io.harness.helpers.ext.vault;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * This is a request DTO class to accommodate the the request data format for Vault services backed by the v2
 * secret engine such as version 0.11.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@OwnedBy(PL)
public class VaultSecretValue {
  private Map<String, String> data;
}
