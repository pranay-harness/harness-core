package software.wings.app;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * The Class PortalConfig.
 */
@Data
public class PortalConfig {
  @JsonProperty(defaultValue = "https://localhost:8000") private String url = "https://localhost:8000";
  private List<String> allowedOrigins = Lists.newArrayList();
  @JsonProperty(defaultValue = "") private String companyName = "";
  @JsonProperty(defaultValue = "/register/verify") private String verificationUrl = "/register/verify";
  @JsonProperty(defaultValue = "/app/%s/overview") private String applicationOverviewUrlPattern = "/app/%s/overview";
  @JsonProperty(defaultValue = "/app/%s/env/%s/execution/%s/detail")
  private String executionUrlPattern = "/app/%s/env/%s/execution/%s/detail";
  private String jwtPasswordSecret;
  private String jwtExternalServiceSecret;
  private String jwtZendeskSecret;
  private String jwtMultiAuthSecret;
  private String jwtSsoRedirectSecret;
  private String jwtAuthSecret;
  private String jwtMarketPlaceSecret;
  private String jwtIdentityServiceSecret;
  private String jwtDataHandlerSecret;
  private String delegateDockerImage;
  private int externalGraphQLRateLimitPerMinute = 500;
  private int customDashGraphQLRateLimitPerMinute = 1000;
  private Long authTokenExpiryInMillis = 24 * 60 * 60 * 1000L;

  /**
   * Gets allowed origins.
   * @return the allowed origins
   */
  @JsonProperty(defaultValue = "")
  public String getAllowedOrigins() {
    return Joiner.on(",").join(allowedOrigins);
  }

  /**
   * Sets allowed orgins.
   *
   * @param allowedOrigins
   */
  public void setAllowedOrigins(String allowedOrigins) {
    this.allowedOrigins = Splitter.on(",").trimResults().omitEmptyStrings().splitToList(allowedOrigins);
  }

  public String getJwtMultiAuthSecret() {
    return jwtMultiAuthSecret;
  }

  public void setJwtMultiAuthSecret(String jwtMultiAuthSecret) {
    this.jwtMultiAuthSecret = jwtMultiAuthSecret;
  }
}
