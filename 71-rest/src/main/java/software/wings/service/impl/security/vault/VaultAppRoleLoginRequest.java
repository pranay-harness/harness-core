package software.wings.service.impl.security.vault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * @author marklu on 2019-04-15
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class VaultAppRoleLoginRequest {
  @JsonProperty("role_id") private String roleId;
  @JsonProperty("secret_id") private String secretId;
}
