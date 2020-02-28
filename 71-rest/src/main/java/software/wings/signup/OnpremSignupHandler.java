package software.wings.signup;

import static org.mindrot.jbcrypt.BCrypt.hashpw;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import software.wings.app.DeployMode;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.dl.WingsPersistence;
import software.wings.resources.UserResource.UpdatePasswordRequest;
import software.wings.service.intfc.SignupHandler;
import software.wings.service.intfc.SignupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.signup.SignupException;

@Slf4j
@Singleton
public class OnpremSignupHandler implements SignupHandler {
  @Inject private SignupService signupService;
  @Inject private UserService userService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MainConfiguration mainConfiguration;

  @Override
  public boolean handle(UserInvite userInvite) {
    validate(userInvite);
    userInvite.setPasswordHash(hashpw(new String(userInvite.getPassword()), BCrypt.gensalt()));
    userService.saveUserInvite(userInvite);
    userService.completeTrialSignupAndSignIn(userInvite);
    return true;
  }

  private void validate(UserInvite userInvite) {
    if (!mainConfiguration.isTrialRegistrationAllowedForBugathon()) {
      validateDeployMode();
      validateCountOfAccount();
    }
    signupService.validateEmail(userInvite.getEmail());
    signupService.validatePassword(userInvite.getPassword());
  }

  private void validateCountOfAccount() {
    if (getAccountCount() > 0) {
      throw new SignupException(
          "An account already exists in the database. This API should only be used for initializing on-prem database");
    }
  }

  private void validateDeployMode() {
    if (!DeployMode.isOnPrem(mainConfiguration.getDeployMode().toString())) {
      throw new SignupException("Method only allowed in case of Onprem deployments");
    }
  }

  @Override
  public User completeSignup(UpdatePasswordRequest updatePasswordRequest, String token) {
    throw new SignupException("This method should not be called in case if Onprem installations.");
  }

  private long getAccountCount() {
    return wingsPersistence.createQuery(Account.class).field(AccountKeys.uuid).notEqual(GLOBAL_ACCOUNT_ID).count();
  }
}