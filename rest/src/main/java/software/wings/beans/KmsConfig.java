package software.wings.beans;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

/**
 * Created by rsingh on 9/29/17.
 */

@Data
public class KmsConfig extends SettingValue implements Encryptable {
  @Attributes(title = "AWS Access Key", required = true) String accessKey;

  @Attributes(title = "AWS Secret Key", required = true) String secretKey;

  @Attributes(title = "AWS key ARN", required = true) String kmsArn;

  @SchemaIgnore @NotEmpty private String accountId;

  public boolean isInitialized() {
    if (StringUtils.isBlank(kmsArn) || StringUtils.isBlank(accessKey) || StringUtils.isBlank(secretKey)) {
      return false;
    }

    if (accessKey.equals("ACCESS_KEY")) {
      return false;
    }

    if (secretKey.equals("SECRET_KEY")) {
      return false;
    }

    return true;
  }

  /**
   * Instantiates a new Splunk config.
   */
  public KmsConfig() {
    super(SettingVariableTypes.KMS.name());
  }
}
