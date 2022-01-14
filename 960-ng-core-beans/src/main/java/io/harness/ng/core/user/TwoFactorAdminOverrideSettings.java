package io.harness.ng.core.user;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(PL)
@Data
@Builder
@AllArgsConstructor
public class TwoFactorAdminOverrideSettings {
  @Getter @Setter private boolean adminOverrideTwoFactorEnabled;
}
