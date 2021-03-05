package software.wings.beans.settings.helm;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.setting.HelmRepoYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class HttpHelmRepoConfigYaml extends HelmRepoYaml {
  private String url;
  private String username;
  private String password;

  @Builder
  public HttpHelmRepoConfigYaml(String type, String harnessApiVersion, String url, String username, String password,
      UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.url = url;
    this.username = username;
    this.password = password;
  }
}
