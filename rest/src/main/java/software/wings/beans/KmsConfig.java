package software.wings.beans;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

/**
 * Created by rsingh on 9/29/17.
 */

@Data
public class KmsConfig extends SettingValue implements Encryptable {
  @Attributes(title = "AWS Access Key", required = true) @Encrypted private char[] accessKey;

  @Attributes(title = "AWS Secret Key", required = true) @Encrypted private char[] secretKey;

  @Attributes(title = "AWS key ARN", required = true) @Encrypted private char[] kmsArn;

  @SchemaIgnore @NotEmpty private String accountId;

  /**
   * Instantiates a new Splunk config.
   */
  public KmsConfig() {
    super(SettingVariableTypes.KMS.name());
  }
}
