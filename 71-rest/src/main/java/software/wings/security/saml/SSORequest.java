package software.wings.security.saml;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.security.authentication.OauthProviderType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SSORequest {
  private OauthProviderType oauthProviderType;
  private String idpRedirectUrl;
  private List<OauthProviderType> oauthProviderTypes;
}
