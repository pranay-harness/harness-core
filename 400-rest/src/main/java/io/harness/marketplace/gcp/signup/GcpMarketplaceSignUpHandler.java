/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.marketplace.gcp.signup;

import java.net.URI;

public interface GcpMarketplaceSignUpHandler {
  URI signUp(String gcpMarketplaceToken);
}
