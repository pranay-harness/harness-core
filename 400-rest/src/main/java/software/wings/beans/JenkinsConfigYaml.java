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
public final class JenkinsConfigYaml extends ArtifactServerYaml {
  private String token;
  private String authMechanism;

  @Builder
  public JenkinsConfigYaml(String type, String harnessApiVersion, String url, String username, String password,
      String token, String authMechanism, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, url, username, password, usageRestrictions);
    this.token = token;
    this.authMechanism = authMechanism;
  }
}
