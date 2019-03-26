package software.wings.security.authentication;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import software.wings.beans.User;
import software.wings.security.authentication.oauth.OauthClient;
import software.wings.security.authentication.oauth.OauthUserInfo;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode(callSuper = true)
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
