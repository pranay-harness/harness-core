package software.wings.settings;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.BaseEntityYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public abstract class SettingValueYaml extends BaseEntityYaml {
  private UsageRestrictionYaml usageRestrictions;

  public SettingValueYaml(String type, String harnessApiVersion, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion);
    this.usageRestrictions = usageRestrictions;
  }
}
