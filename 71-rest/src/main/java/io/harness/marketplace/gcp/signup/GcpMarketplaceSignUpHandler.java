package io.harness.marketplace.gcp.signup;

import java.net.URI;

public interface GcpMarketplaceSignUpHandler {
  /**
   * @return url to redirect to
   */
  URI signUp(String gcpMarketplaceToken);
}
