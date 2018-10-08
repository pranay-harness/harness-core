package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.annotation.Encrypted;
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

/**
 * Created by anubhaw on 12/27/16.
 */
@JsonTypeName("AWS")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@ToString(exclude = "secretKey")
public class AwsConfig extends SettingValue implements EncryptableSetting {
  @Attributes(title = "Access Key", required = true) @NotEmpty private String accessKey;

  @Attributes(title = "Secret Key", required = true) @Encrypted private char[] secretKey;

  @SchemaIgnore @NotEmpty private String accountId; // internal

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedSecretKey;
  /**
   * Instantiates a new Aws config.
   */
  public AwsConfig() {
    super(SettingVariableTypes.AWS.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public AwsConfig(String accessKey, char[] secretKey, String accountId, String encryptedSecretKey) {
    this();
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.accountId = accountId;
    this.encryptedSecretKey = encryptedSecretKey;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends CloudProviderYaml {
    private String accessKey;
    private String secretKey;

    @Builder
    public Yaml(String type, String harnessApiVersion, String accessKey, String secretKey,
        UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.accessKey = accessKey;
      this.secretKey = secretKey;
    }
  }
}
