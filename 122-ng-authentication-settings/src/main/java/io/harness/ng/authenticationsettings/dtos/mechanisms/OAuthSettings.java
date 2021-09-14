/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.authenticationsettings.dtos.mechanisms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.account.AuthenticationMechanism;
import io.harness.ng.core.account.OauthProviderType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("OAUTH")
@OwnedBy(HarnessTeam.PL)
public class OAuthSettings extends NGAuthSettings {
  private String filter;
  private Set<OauthProviderType> allowedProviders;

  public OAuthSettings(@JsonProperty("filter") String filter,
      @JsonProperty("allowedProviders") Set<OauthProviderType> allowedProviders) {
    super(AuthenticationMechanism.OAUTH);
    this.filter = filter;
    this.allowedProviders = allowedProviders;
  }

  @Override
  public AuthenticationMechanism getSettingsType() {
    return AuthenticationMechanism.OAUTH;
  }
}
