package software.wings.beans;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import software.wings.security.UsageRestrictions;
import software.wings.yaml.setting.CloudProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class PcfConfigYaml extends CloudProviderYaml {
  private String endpointUrl;
  private String username;
  private String usernameSecretId;
  private String password = ENCRYPTED_VALUE_STR;
  private boolean skipValidation;

  @Builder
  public PcfConfigYaml(String type, String harnessApiVersion, String endpointUrl, String username,
      String usernameSecretId, String password, boolean skipValidation, UsageRestrictions.Yaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.endpointUrl = endpointUrl;
    this.username = username;
    this.usernameSecretId = usernameSecretId;
    this.password = password;
    this.skipValidation = skipValidation;
  }
}
