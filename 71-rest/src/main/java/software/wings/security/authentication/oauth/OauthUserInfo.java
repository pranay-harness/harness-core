package software.wings.security.authentication.oauth;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@Builder
public class OauthUserInfo {
  String email;
  String name;
  String login;
}
