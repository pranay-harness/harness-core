package io.harness.ng.authenticationsettings.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.authenticationsettings.dtos.mechanisms.NGAuthSettings;
import io.harness.ng.core.account.AuthenticationMechanism;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(HarnessTeam.PL)
public class AuthenticationSettingsResponse {
  List<NGAuthSettings> ngAuthSettings;
  Set<String> whitelistedDomains;
  AuthenticationMechanism authenticationMechanism;
  boolean twoFactorEnabled;
}
