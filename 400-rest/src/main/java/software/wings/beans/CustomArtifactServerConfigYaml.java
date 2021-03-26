package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.yaml.setting.ArtifactServerYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class CustomArtifactServerConfigYaml extends ArtifactServerYaml {
  @Builder
  public CustomArtifactServerConfigYaml(
      String type, String harnessApiVersion, UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, null, null, null, usageRestrictions);
  }
}
