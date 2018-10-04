package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.CloudProviderYaml;

@JsonTypeName("AZURE")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@ToString(exclude = "key")
public class AzureConfig extends SettingValue implements Encryptable {
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

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public AzureConfig(String clientId, String tenantId, char[] key, String accountId, String encryptedKey) {
    this();
    this.clientId = clientId;
    this.tenantId = tenantId;
    this.key = key;
    this.accountId = accountId;
    this.encryptedKey = encryptedKey;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends CloudProviderYaml {
    private String clientId;
    private String tenantId;
    private String key;

    @Builder
    public Yaml(String type, String harnessApiVersion, String clientId, String tenantId, String key,
        UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.clientId = clientId;
      this.tenantId = tenantId;
      this.key = key;
    }
  }
}
