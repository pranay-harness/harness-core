package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.CloudProviderYaml;

@JsonTypeName("AZURE")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@ToString(exclude = "key")
public class AzureConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Client ID", required = true) @NotEmpty private String clientId;

  @Attributes(title = "Tenant ID", required = true) @NotEmpty private String tenantId;

  @Attributes(title = "Key", required = true) @NotEmpty private String key;

  @SchemaIgnore @NotEmpty private String accountId;

  /**
   * Instantiates a new Azure config.
   */
  public AzureConfig() {
    super(SettingVariableTypes.AZURE.name());
  }

  public AzureConfig(String clientId, String tenantId, String key, String accountId) {
    this();
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.key = key;
    this.accountId = accountId;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends CloudProviderYaml {
    private String clientId;
    private String tenantId;
    private String key;

    @Builder
    public Yaml(
        String type, String harnessApiVersion, String clientId, String tenantId, String key, String subscriptionId) {
      super(type, harnessApiVersion);
      this.clientId = clientId;
      this.tenantId = tenantId;
      this.key = key;
    }
  }
}
