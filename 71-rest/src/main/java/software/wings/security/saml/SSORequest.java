package software.wings.security.saml;

import lombok.Data;
import software.wings.security.authentication.OauthProviderType;

@Data
public class SSORequest {
  private OauthProviderType oauthProviderType;
  private String idpRedirectUrl;
}
