package io.harness.marketplace.gcp.signup;

import java.net.URI;

public interface GcpMarketplaceSignUpHandler {
  URI signUp(String gcpMarketplaceToken);
}
