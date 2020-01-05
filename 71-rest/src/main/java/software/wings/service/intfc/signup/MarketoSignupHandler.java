package software.wings.service.intfc.signup;

import static org.mindrot.jbcrypt.BCrypt.hashpw;

import com.google.inject.Inject;

import io.harness.event.handler.impl.EventPublishHelper;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import software.wings.app.MainConfiguration;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInviteSource;
import software.wings.beans.UserInviteSource.SourceType;
import software.wings.resources.UserResource.UpdatePasswordRequest;
import software.wings.service.intfc.SignupHandler;
import software.wings.service.intfc.SignupService;
import software.wings.service.intfc.UserService;

@Slf4j
public class MarketoSignupHandler implements SignupHandler {
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private SignupSpamChecker spamChecker;
  @Inject private SignupService signupService;
  @Inject private UserService userService;
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public boolean handle(UserInvite userInvite) {
    final String emailAddress = userInvite.getEmail().toLowerCase();
    signupService.validateCluster();
    signupService.validateEmail(emailAddress);

    UserInvite userInviteInDB = signupService.getUserInviteByEmail(emailAddress);

    if (userInviteInDB == null) {
      userInvite.setSource(UserInviteSource.builder().type(SourceType.MARKETO_LINKEDIN).build());
      userInvite.setCompleted(false);
      String inviteId = userService.saveUserInvite(userInvite);
      userInvite.setUuid(inviteId);

      // Send an email invitation for the trial user to finish up the sign-up with asking password.
      signupService.sendPasswordSetupMailForSignup(userInvite);
      eventPublishHelper.publishTrialUserSignupEvent(emailAddress, userInvite.getName(), inviteId);
    } else if (userInviteInDB.isCompleted()) {
      if (spamChecker.isSpam(userInviteInDB)) {
        return false;
      }
      // HAR-7590: If user invite has completed. Send an email saying so and ask the user to login directly.
      signupService.sendTrialSignupCompletedEmail(userInviteInDB);
    } else {
      if (spamChecker.isSpam(userInviteInDB)) {
        return false;
      }
      // HAR-7250: If the user invite was not completed. Resend the verification/invitation email.
      signupService.sendPasswordSetupMailForSignup(userInvite);
    }
    return true;
  }

  @Override
  public User completeSignup(UpdatePasswordRequest passwordRequest, String token) {
    String email = signupService.getEmail(token);
    UserInvite userInvite = signupService.getUserInviteByEmail(email);
    signupService.checkIfUserInviteIsValid(userInvite, email);
    return setPasswordAndCompleteSignup(passwordRequest, userInvite);
  }

  private User setPasswordAndCompleteSignup(UpdatePasswordRequest passwordRequest, UserInvite userInvite) {
    char[] password = passwordRequest.getPassword().toCharArray();
    signupService.validatePassword(password);
    userInvite.setPassword(password);
    userInvite.setPasswordHash(hashpw(new String(userInvite.getPassword()), BCrypt.gensalt()));
    userService.saveUserInvite(userInvite);

    // No user and account is created till here. Once this call is made, only then the account and user's are created.
    // This call returns a user object setting bearer token in it and directly logs in the user.
    return userService.completeTrialSignupAndSignIn(userInvite);
  }
}
