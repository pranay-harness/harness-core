package software.wings.security.authentication;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * @author Vaibhav Tulsyan
 * 12/Jun/2019
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSettingsResponse {
  private AuthenticationMechanism authenticationMechanism;
  // null or empty means no email domain restrictions
  private Set<String> allowedDomains;
  private Set<OauthProviderType> oauthProviderTypes;
}
