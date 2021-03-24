package software.wings.beans.config;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.yaml.setting.ArtifactServerYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class ArtifactoryConfigYaml extends ArtifactServerYaml {
  @Builder
  public ArtifactoryConfigYaml(String type, String harnessApiVersion, String url, String username, String password,
      UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, url, username, password, usageRestrictions);
  }
}
