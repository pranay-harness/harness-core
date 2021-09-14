/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.signup.services;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.WingsException;
import io.harness.ng.core.user.UserInfo;
import io.harness.signup.dto.OAuthSignupDTO;
import io.harness.signup.dto.SignupDTO;
import io.harness.signup.dto.VerifyTokenResponseDTO;

@OwnedBy(GTM)
public interface SignupService {
  UserInfo signup(SignupDTO dto, String captchaToken) throws WingsException;

  boolean createSignupInvite(SignupDTO dto, String captchaToken);

  UserInfo completeSignupInvite(String token);

  UserInfo oAuthSignup(OAuthSignupDTO dto) throws WingsException;

  VerifyTokenResponseDTO verifyToken(String token);

  void resendVerificationEmail(String email);
}
