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
public final class BambooConfigYaml extends ArtifactServerYaml {
  @Builder
  public BambooConfigYaml(String type, String harnessApiVersion, String url, String username, String password,
      UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, url, username, password, usageRestrictions);
  }
}
