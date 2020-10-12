package io.harness.helpers.ext.vault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * @author marklu on 2019-04-15
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class VaultAppRoleLoginResponse {
  private VaultAppRoleLoginResult auth;
}
