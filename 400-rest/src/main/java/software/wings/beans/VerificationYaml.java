package software.wings.beans;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.yaml.setting.VerificationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class VerificationYaml extends VerificationProviderYaml {
  private String url;
  private String username;
  private String password = ENCRYPTED_VALUE_STR;
  private String token = ENCRYPTED_VALUE_STR;
  private String authMechanism;

  @Builder
  public VerificationYaml(String type, String harnessApiVersion, String url, String username, String password,
      String token, String authMechanism, UsageRestrictionsYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.url = url;
    this.username = username;
    this.password = password;
    this.authMechanism = authMechanism;
    this.token = token;
  }
}
