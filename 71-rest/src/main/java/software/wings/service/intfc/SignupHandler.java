package software.wings.service.intfc;

import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.resources.UserResource.UpdatePasswordRequest;

public interface SignupHandler {
  boolean handle(UserInvite userInvite);

  User completeSignup(UpdatePasswordRequest updatePasswordRequest, String token);
}
