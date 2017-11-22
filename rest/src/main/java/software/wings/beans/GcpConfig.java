package software.wings.beans;

import static software.wings.settings.SettingValue.SettingVariableTypes.GCP;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.CloudProviderYaml;

/**
 * Created by bzane on 2/28/17
 */
@JsonTypeName("GCP")
@Data
@Builder
public class GcpConfig extends SettingValue implements Encryptable {
  @JsonIgnore @NotEmpty @Encrypted private char[] serviceAccountKeyFileContent;

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedServiceAccountKeyFileContent;

  public GcpConfig() {
    super(GCP.name());
  }

  public GcpConfig(
      char[] serviceAccountKeyFileContent, String accountId, String encryptedServiceAccountKeyFileContent) {
    this();
    this.serviceAccountKeyFileContent = serviceAccountKeyFileContent;
    this.accountId = accountId;
    this.encryptedServiceAccountKeyFileContent = encryptedServiceAccountKeyFileContent;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends CloudProviderYaml {
    private String serviceAccountKeyFileContent;

    public Yaml() {}

    public Yaml(String type, String name, String serviceAccountKeyFileContent) {
      super(type, name);
      this.serviceAccountKeyFileContent = serviceAccountKeyFileContent;
    }
  }
}
