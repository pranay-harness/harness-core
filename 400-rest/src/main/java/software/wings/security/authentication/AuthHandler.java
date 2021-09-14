/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.account.AuthenticationMechanism;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

@OwnedBy(PL)
public interface AuthHandler {
  /***
   * Check if a user's credentials are valid
   * @param credentials
   * @return
   */
  AuthenticationResponse authenticate(String... credentials)
      throws URISyntaxException, InterruptedException, ExecutionException, IOException;

  AuthenticationMechanism getAuthenticationMechanism();
}
