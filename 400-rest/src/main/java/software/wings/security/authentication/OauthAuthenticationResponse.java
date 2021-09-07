package software.wings.security.authentication;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.User;
import software.wings.security.authentication.oauth.OauthClient;
import software.wings.security.authentication.oauth.OauthUserInfo;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode(callSuper = true)
@TargetModule(HarnessModule._950_NG_AUTHENTICATION_SERVICE)
public class OauthAuthenticationResponse extends AuthenticationResponse {
  OauthUserInfo oauthUserInfo;
  Boolean userFoundInDB;
  OauthClient oauthClient;

  @Builder
  public OauthAuthenticationResponse(
      User user, OauthUserInfo oauthUserInfo, Boolean userFoundInDB, OauthClient oauthClient) {
    super(user);
    this.oauthUserInfo = oauthUserInfo;
    this.userFoundInDB = userFoundInDB;
    this.oauthClient = oauthClient;
  }
}
