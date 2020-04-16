package software.wings.security.encryption.setupusage;

import io.harness.persistence.UuidAware;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import software.wings.settings.SettingValue.SettingVariableTypes;

@Value
@Builder
@EqualsAndHashCode(exclude = "entity")
public class SecretSetupUsage {
  @NonNull private String entityId;
  @NonNull private SettingVariableTypes type;
  private String fieldName;
  private UuidAware entity;
}
