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

import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.resources.UserResource.UpdatePasswordRequest;

@OwnedBy(GTM)
@TargetModule(_950_NG_SIGNUP)
public interface SignupHandler {
  boolean handle(UserInvite userInvite);
  User completeSignup(String token);
  User completeSignup(UpdatePasswordRequest passwordRequest, String token);
}
