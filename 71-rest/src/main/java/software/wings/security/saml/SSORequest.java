package software.wings.security.saml;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.security.authentication.OauthProviderType;

import java.util.List;

@OwnedBy(PL)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SSORequest {
  private OauthProviderType oauthProviderType;
  private String idpRedirectUrl;
  private List<OauthProviderType> oauthProviderTypes;
}
