package software.wings.beans;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.setting.ArtifactServerYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class CustomArtifactServerConfigYaml extends ArtifactServerYaml {
  @Builder
  public CustomArtifactServerConfigYaml(String type, String harnessApiVersion, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, null, null, null, usageRestrictions);
  }
}
