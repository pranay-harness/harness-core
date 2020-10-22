package io.harness.helpers.ext.vault;

import static io.harness.annotations.dev.HarnessTeam.PL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@OwnedBy(PL)
public class VaultReadResponse {
  private Map<String, String> data;
}
