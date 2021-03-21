package software.wings.beans.settings.helm;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictions;
import software.wings.yaml.setting.HelmRepoYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class HttpHelmRepoConfigYaml extends HelmRepoYaml {
  private String url;
  private String username;
  private String password;

  @Builder
  public HttpHelmRepoConfigYaml(String type, String harnessApiVersion, String url, String username, String password,
      UsageRestrictions.Yaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.url = url;
    this.username = username;
    this.password = password;
  }
}
