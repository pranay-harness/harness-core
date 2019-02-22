package software.wings.security.authentication.oauth;

import com.google.inject.Singleton;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Singleton
public class LinkedinConfig {
  private String callbackUrl;
  private String clientId;
  private String clientSecret;
}
