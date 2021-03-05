package software.wings.beans;

import software.wings.security.UsageRestrictionYaml;
import software.wings.settings.SettingValueYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class HostConnectionAttributesYaml extends SettingValueYaml {
  private String connectionType;
  private String accessType;
  private String userName;
  private String key;

  @lombok.Builder
  public HostConnectionAttributesYaml(String type, String harnessApiVersion, String connectionType, String accessType,
      String userName, String key, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.connectionType = connectionType;
    this.accessType = accessType;
    this.userName = userName;
    this.key = key;
  }
}
