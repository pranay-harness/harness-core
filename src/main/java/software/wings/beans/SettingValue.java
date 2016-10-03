package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Created by anubhaw on 5/16/16.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class SettingValue {
  @JsonTypeId private SettingVariableTypes type;

  /**
   * Instantiates a new setting value.
   *
   * @param type the type
   */
  public SettingValue(SettingVariableTypes type) {
    this.type = type;
  }

  /**
   * Gets type.
   *
   * @return the type
   */
  public SettingVariableTypes getType() {
    return type;
  }

  /**
   * Sets type.
   *
   * @param type the type
   */
  public void setType(SettingVariableTypes type) {
    this.type = type;
  }

  /**
   * The Enum SettingVariableTypes.
   */
  public enum SettingVariableTypes {
    /**
     * Host connection attributes setting variable types.
     */
    HOST_CONNECTION_ATTRIBUTES,

    /**
     * Bastion host connection attributes setting variable types.
     */
    BASTION_HOST_CONNECTION_ATTRIBUTES,

    /**
     * Smtp setting variable types.
     */
    SMTP, /**
           * Jenkins setting variable types.
           */
    JENKINS,

    /**
     * String setting variable types.
     */
    STRING,

    /**
     * Splunk setting variable types.
     */
    SPLUNK,

    /**
     * App dynamics setting variable types.
     */
    APP_DYNAMICS,

    /**
     * Elastic Load Balancer Settings
     */
    ELB
  }
}
