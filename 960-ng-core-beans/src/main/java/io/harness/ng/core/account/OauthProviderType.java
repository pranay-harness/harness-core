package io.harness.ng.core.account;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

/**
 * @author marklu on 2019-05-11
 */
@OwnedBy(HarnessTeam.PL)
public enum OauthProviderType {
  AZURE,
  BITBUCKET,
  GITHUB,
  GITLAB,
  GOOGLE,
  LINKEDIN;
}
