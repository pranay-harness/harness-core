package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@FieldNameConstants(innerTypeName = "AccountPreferencesKeys")
@TargetModule(HarnessModule._955_ACCOUNT_MGMT)
@OwnedBy(HarnessTeam.PL)
public class AccountPreferences {
  Integer delegateSecretsCacheTTLInHours = 1;
}
