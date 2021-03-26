package software.wings.beans;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.yaml.setting.CollaborationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class JiraConfigYaml extends CollaborationProviderYaml {
  private String baseUrl;
  private String username;
  private String password = ENCRYPTED_VALUE_STR;

  @Builder
  public JiraConfigYaml(String type, String harnessApiVersion, String baseUrl, String username, String password,
      UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.baseUrl = baseUrl;
    this.username = username;
    this.password = password;
  }
}
