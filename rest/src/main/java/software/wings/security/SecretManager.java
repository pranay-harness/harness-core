package software.wings.security;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.app.MainConfiguration;

@Singleton
public class SecretManager {
  @Inject private MainConfiguration configuration;

  public enum JWT_CATEGORY {
    MULTIFACTOR_AUTH(3 * 60 * 1000), // 3 mins
    SSO_REDIRECT(60 * 1000), // 1 min
    PASSWORD_SECRET(4 * 60 * 60 * 1000), // 4 hrs
    ZENDESK_SECRET(4 * 60 * 60 * 1000), // 4 hrs
    EXTERNAL_SERVICE_SECRET(60 * 60 * 1000), // 1hr
    AUTH_SECRET(24 * 60 * 60 * 1000); // 24 hr

    private int validityDuration;

    JWT_CATEGORY(int validityDuration) {
      this.validityDuration = validityDuration;
    }

    public int getValidityDuration() {
      return validityDuration;
    }
  }

  public String getJWTSecret(JWT_CATEGORY category) {
    switch (category) {
      case MULTIFACTOR_AUTH:
        return configuration.getPortal().getJwtMultiAuthSecret();
      case ZENDESK_SECRET:
        return configuration.getPortal().getJwtZendeskSecret();
      case PASSWORD_SECRET:
        return configuration.getPortal().getJwtPasswordSecret();
      case EXTERNAL_SERVICE_SECRET:
        return configuration.getPortal().getJwtExternalServiceSecret();
      case SSO_REDIRECT:
        return configuration.getPortal().getJwtSsoRedirectSecret();
      case AUTH_SECRET:
        return configuration.getPortal().getJwtAuthSecret();
      default:
        return configuration.getPortal().getJwtMultiAuthSecret();
    }
  }
}
