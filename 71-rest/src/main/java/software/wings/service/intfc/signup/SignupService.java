package software.wings.service.intfc.signup;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.User;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInviteSource.SourceType;
import software.wings.resources.UserResource.UpdatePasswordRequest;

@Singleton
public class SignupService {
  @Inject MarketoSignupHandler marketoSignupHandler;

  public boolean signup(UserInvite userInvite, String source) {
    SourceType sourceType = SourceType.valueOf(source);
    switch (sourceType) {
      case MARKETO_LINKEDIN:
        return marketoSignupHandler.handle(userInvite);
      default:
        throw new SignupException(String.format("Incorrect source type provided: %s", userInvite.getSource()));
    }
  }

  public User completeSignup(UpdatePasswordRequest passwordRequest, String resetPasswordToken) {
    return marketoSignupHandler.completeSignup(passwordRequest, resetPasswordToken);
  }
}
