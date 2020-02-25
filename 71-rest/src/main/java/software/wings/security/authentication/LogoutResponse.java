package software.wings.security.authentication;

import lombok.Data;

@Data
public class LogoutResponse {
  private String logoutUrl;
}
