package software.wings.service.impl.security.vault;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author marklu on 2019-04-15
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VaultAppRoleLoginResult {
  private boolean renewable;
  @JsonProperty("lease_duration") private long leaseDuration;
  private List<String> policies;
  private String accessor;
  @JsonProperty("client_token") private String clientToken;
}
