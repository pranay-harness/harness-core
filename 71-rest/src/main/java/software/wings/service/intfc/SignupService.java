package software.wings.service.intfc;

import software.wings.beans.UserInvite;

import java.io.UnsupportedEncodingException;
import java.util.Map;

public interface SignupService {
  void sendTrialSignupCompletedEmail(UserInvite userInvite);

  void sendEmail(UserInvite userInvite, String templateName, Map<String, String> templateModel);

  UserInvite getUserInviteByEmail(String email);

  void validateEmail(String email);

  void validateName(String name);

  void validateCluster();

  void checkIfEmailIsValid(String email);

  void sendPasswordSetupMailForSignup(UserInvite userInvite);

  String createSignupTokeFromSecret(String jwtPasswordSecret, String email, int expireAfterDays)
      throws UnsupportedEncodingException;

  void validatePassword(char[] password);

  String getEmail(String token);

  void checkIfUserInviteIsValid(UserInvite userInvite, String email);
}
