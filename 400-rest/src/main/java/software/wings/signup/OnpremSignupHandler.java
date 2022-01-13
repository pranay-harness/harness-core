/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.signup;

import static io.harness.annotations.dev.HarnessModule._950_NG_SIGNUP;
import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;

import static org.mindrot.jbcrypt.BCrypt.hashpw;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.configuration.DeployMode;
import io.harness.exception.SignupException;

import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.dl.WingsPersistence;
import software.wings.resources.UserResource.UpdatePasswordRequest;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.SignupHandler;
import software.wings.service.intfc.SignupService;
import software.wings.service.intfc.UserService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;

@OwnedBy(PL)
@TargetModule(_950_NG_SIGNUP)
@Slf4j
@Singleton
public class OnpremSignupHandler implements SignupHandler {
  @Inject private SignupService signupService;
  @Inject private UserService userService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private AccountService accountService;

  @Override
  public boolean handle(UserInvite userInvite) {
    validate(userInvite);
    userInvite.setPasswordHash(hashpw(new String(userInvite.getPassword()), BCrypt.gensalt()));
    userService.saveUserInvite(userInvite);
    userService.completeTrialSignupAndSignIn(userInvite);
    accountService.updateFeatureFlagsForOnPremAccount();

    return true;
  }

  private void validate(UserInvite userInvite) {
    if (!mainConfiguration.isTrialRegistrationAllowedForBugathon()) {
      validateDeployMode();
      validateCountOfAccount();
    }
    signupService.validateEmail(userInvite.getEmail());
    signupService.validateName(userInvite.getName());
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

  @Override
  public User completeSignup(String token) {
    throw new UnsupportedOperationException("Operation not supported");
  }

  private long getAccountCount() {
    return wingsPersistence.createQuery(Account.class).field(AccountKeys.uuid).notEqual(GLOBAL_ACCOUNT_ID).count();
  }
}
