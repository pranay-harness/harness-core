package software.wings.helpers.ext.mail;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import software.wings.security.UsageRestrictionYaml;
import software.wings.yaml.setting.CollaborationProviderYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class SmtpConfigYaml extends CollaborationProviderYaml {
  private String host;
  private int port;
  private String fromAddress;
  private boolean useSSL;
  private String username;
  private String password = ENCRYPTED_VALUE_STR;

  @Builder
  public SmtpConfigYaml(String type, String harnessApiVersion, String host, int port, String fromAddress,
      boolean useSSL, String username, String password, UsageRestrictionYaml usageRestrictions) {
    super(type, harnessApiVersion, usageRestrictions);
    this.host = host;
    this.port = port;
    this.fromAddress = fromAddress;
    this.useSSL = useSSL;
    this.username = username;
    this.password = password;
  }
}
