/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.Account;
import software.wings.beans.User;

@OwnedBy(PL)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public interface TwoFactorAuthHandler {
  /***
   * Check if a user's credentials are valid . The user should already have been authenticated once by the primary
   * authentication mechanism
   * @param params
   * @return
   */
  User authenticate(User user, String... credentials);

  TwoFactorAuthenticationMechanism getAuthenticationMechanism();

  TwoFactorAuthenticationSettings createTwoFactorAuthenticationSettings(User user, Account accouunt);

  User applyTwoFactorAuthenticationSettings(User user, TwoFactorAuthenticationSettings settings);

  User disableTwoFactorAuthentication(User user);

  boolean resetAndSendEmail(User user);

  boolean sendTwoFactorAuthenticationResetEmail(User user);
}
