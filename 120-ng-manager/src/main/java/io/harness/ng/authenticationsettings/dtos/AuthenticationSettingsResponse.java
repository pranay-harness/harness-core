package io.harness.ng.authenticationsettings.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.authenticationsettings.dtos.mechanisms.NGAuthSettings;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.PL)
public class AuthenticationSettingsResponse {
  List<NGAuthSettings> ngAuthSettings;
  Set<String> whitelistedDomains;
  boolean twoFactorEnabled;
}
