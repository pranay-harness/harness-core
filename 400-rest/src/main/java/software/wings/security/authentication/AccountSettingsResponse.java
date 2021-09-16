package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.ng.core.account.OauthProviderType;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Vaibhav Tulsyan
 * 12/Jun/2019
 */
@OwnedBy(PL)
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
