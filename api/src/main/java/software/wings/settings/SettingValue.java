package software.wings.settings;

import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ro.fortsoft.pf4j.ExtensionPoint;

/**
 * Created by anubhaw on 5/16/16.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public abstract class SettingValue implements ExtensionPoint {
  @JsonTypeId private String type;

  /**
   * Instantiates a new setting value.
   *
   * @param type the type
   */
  public SettingValue(String type) {
    this.type = type;
  }

  /**
   * Gets type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets type.
   *
   * @param type the type
   */
  public void setType(String type) {
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
    JENKINS, /**
              * Bamboo setting variable types.
              */
    BAMBOO,

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
    ELB,

    /**
     * Aws credentials setting variable types.
     */
    AWS_CREDENTIALS,

    /**
     * Slack setting variable types.
     */
    SLACK, /**
            * Aws setting variable types.
            */
    AWS, /**
          * Physical data center setting variable types.
          */
    PHYSICAL_DATA_CENTER
  }
}
