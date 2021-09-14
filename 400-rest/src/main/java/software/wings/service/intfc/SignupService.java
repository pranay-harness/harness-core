/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.intfc;

import static io.harness.annotations.dev.HarnessModule._950_NG_SIGNUP;
import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.UserInvite;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@OwnedBy(GTM)
@TargetModule(_950_NG_SIGNUP)
public interface SignupService {
  void sendTrialSignupCompletedEmail(UserInvite userInvite);

  void sendLinkedInTrialSignupCompletedEmail(UserInvite userInvite, String generatedPassword);

  void sendEmail(UserInvite userInvite, String templateName, Map<String, String> templateModel);

  UserInvite getUserInviteByEmail(String email);

  void validateEmail(String email);

  void validateName(String name);

  void validateCluster();

  void checkIfEmailIsValid(String email);

  void sendLinkedInSignupVerificationEmail(UserInvite userInvite);

  void sendTrialSignupVerificationEmail(UserInvite userInvite, Map<String, String> templateModel);

  String createSignupTokenFromSecret(String jwtPasswordSecret, String email, int expireAfterDays)
      throws UnsupportedEncodingException;

  void validatePassword(char[] password);

  String getEmail(String token);

  void checkIfUserInviteIsValid(UserInvite userInvite, String email);
}
