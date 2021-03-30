package io.harness.ng.authenticationsettings.dtos.mechanisms;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.security.authentication.OauthProviderType;

import java.util.Set;

@OwnedBy(HarnessTeam.PL)
public class OAuthSettings extends NGAuthSettings {
  private String filter;
  private Set<OauthProviderType> allowedProviders;
}
