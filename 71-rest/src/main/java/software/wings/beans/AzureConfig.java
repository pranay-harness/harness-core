package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.CloudProviderYaml;

@JsonTypeName("AZURE")
@Data
@Builder
@ToString(exclude = "key")
@EqualsAndHashCode(callSuper = false)
public class AzureConfig extends SettingValue implements EncryptableSetting {
  @Attributes(title = "Client ID [Application ID]", required = true) @NotEmpty private String clientId;

  @Attributes(title = "Tenant ID [Directory ID]", required = true) @NotEmpty private String tenantId;

  @Attributes(title = "Key", required = true) @Encrypted private char[] key;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedKey;

  /**
   * Instantiates a new Azure config.
   */
  public AzureConfig() {
    super(SettingVariableTypes.AZURE.name());
  }

  public AzureConfig(String clientId, String tenantId, char[] key, String accountId, String encryptedKey) {
    this();
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.key = key == null ? null : key.clone();
    this.accountId = accountId;
    this.encryptedKey = encryptedKey;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends CloudProviderYaml {
    private String clientId;
    private String tenantId;
    private String key;

    @Builder
    public Yaml(String type, String harnessApiVersion, String clientId, String tenantId, String key,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.clientId = clientId;
      this.tenantId = tenantId;
      this.key = key;
    }
  }
}
