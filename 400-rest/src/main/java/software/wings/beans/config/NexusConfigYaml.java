package software.wings.beans.config;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.setting.ArtifactServerYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class NexusConfigYaml extends ArtifactServerYaml {
  private String version;
  @Builder
  public NexusConfigYaml(String type, String harnessApiVersion, String url, String username, String password,
      String version, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, url, username, password, usageRestrictions);
    this.version = version;
  }
}
