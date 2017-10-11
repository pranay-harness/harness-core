package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

/**
 * Created by anubhaw on 12/27/16.
 */
@JsonTypeName("AWS")
@Data
@Builder
@ToString(exclude = "secretKey")
public class AwsConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Access Key", required = true) @NotEmpty private String accessKey;

  @Attributes(title = "Secret Key", required = true) @NotEmpty @Encrypted private char[] secretKey;

  @SchemaIgnore @NotEmpty private String accountId; // internal

  @SchemaIgnore private String encryptedSecretKey;
  /**
   * Instantiates a new Aws config.
   */
  public AwsConfig() {
    super(SettingVariableTypes.AWS.name());
  }

  public AwsConfig(String accessKey, char[] secretKey, String accountId, String encryptedSecretKey) {
    this();
    this.accessKey = accessKey;
    this.secretKey = secretKey;
    this.accountId = accountId;
    this.encryptedSecretKey = encryptedSecretKey;
  }
}
