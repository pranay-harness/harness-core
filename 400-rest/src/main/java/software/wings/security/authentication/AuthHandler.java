/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessModule._950_NG_AUTHENTICATION_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ng.core.account.AuthenticationMechanism;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

@OwnedBy(PL)
@TargetModule(_950_NG_AUTHENTICATION_SERVICE)
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
