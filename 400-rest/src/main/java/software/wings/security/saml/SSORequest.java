package software.wings.security.saml;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ng.core.account.OauthProviderType;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class SSORequest {
  private OauthProviderType oauthProviderType;
  private String idpRedirectUrl;
  private List<OauthProviderType> oauthProviderTypes;
}
