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
public final class SmbConfigYaml extends ArtifactServerYaml {
  String domain;
  @Builder
  public SmbConfigYaml(String type, String harnessApiVersion, String url, String domain, String username,
      String password, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, url, username, password, usageRestrictions);
    this.domain = domain;
  }
}
