package io.harness.account;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AccountConfig is used to store specific global account information
 */

@OwnedBy(HarnessTeam.GTM)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountConfig {
  @JsonProperty("deploymentClusterName") private String deploymentClusterName;
}
