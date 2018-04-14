package software.wings.security.authentication;

import lombok.Data;

@Data
public class SsoRedirectRequest {
  String jwtToken;
}
