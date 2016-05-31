package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import software.wings.helpers.ext.mail.SmtpConfig;

/**
 * Created by anubhaw on 5/16/16.
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @Type(value = HostConnectionAttributes.class, name = "HOST_CONNECTION_ATTRIBUTES")
  , @Type(value = BastionConnectionAttributes.class, name = "BASTION_HOST_CONNECTION_ATTRIBUTES"),
      @Type(value = SmtpConfig.class, name = "SMTP"), @Type(value = JenkinsConfig.class, name = "JENKINS")
})
public abstract class SettingValue {
  private SettingVariableTypes type;

  public SettingValue(SettingVariableTypes type) {
    this.type = type;
  }

  public SettingVariableTypes getType() {
    return type;
  }

  public void setType(SettingVariableTypes type) {
    this.type = type;
  }

  public enum SettingVariableTypes { HOST_CONNECTION_ATTRIBUTES, BASTION_HOST_CONNECTION_ATTRIBUTES, SMTP, JENKINS }
}
