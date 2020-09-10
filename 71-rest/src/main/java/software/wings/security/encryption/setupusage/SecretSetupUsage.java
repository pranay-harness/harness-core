package software.wings.security.encryption.setupusage;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import software.wings.settings.SettingVariableTypes;

@OwnedBy(PL)
@Value
@Builder
@EqualsAndHashCode(exclude = "entity")
public class SecretSetupUsage {
  @NonNull private String entityId;
  @NonNull private SettingVariableTypes type;
  private String fieldName;
  private UuidAware entity;
}
