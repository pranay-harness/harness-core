package software.wings.security.authentication;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

/**
 * @author marklu on 2019-05-11
 */
@OwnedBy(HarnessTeam.PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public enum OauthProviderType {
  AZURE,
  BITBUCKET,
  GITHUB,
  GITLAB,
  GOOGLE,
  LINKEDIN;
}
