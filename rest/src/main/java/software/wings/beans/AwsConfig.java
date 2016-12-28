package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import software.wings.settings.SettingValue;

/**
 * Created by anubhaw on 12/27/16.
 */

@JsonTypeName("AWS")
public class AwsConfig extends SettingValue {
  @Attributes(title = "Access key") private String accessKey;
  @Attributes(title = "Secret key") private String secretKey;

  public AwsConfig() {
    super(SettingVariableTypes.AWS.name());
  }

  public String getAccessKey() {
    return accessKey;
  }

  public void setAccessKey(String accessKey) {
    this.accessKey = accessKey;
  }

  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
    this.secretKey = secretKey;
  }
}
