package software.wings.settings;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import ro.fortsoft.pf4j.ExtensionPoint;

/**
 * Created by anubhaw on 5/16/16.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = As.EXISTING_PROPERTY)
public abstract class SettingValue implements ExtensionPoint {
  private String type;

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
   * Do validate boolean.
   *
   * @return the boolean
   */
  public abstract boolean doValidate();

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
    SMTP,

    /**
     * Jenkins setting variable types.
     */
    JENKINS,

    /**
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
    APP_DYNAMICS, /**
                   * Slack setting variable types.
                   */
    SLACK,

    /**
     * AWS setting variable types.
     */
    AWS,

    /**
     * GCP setting variable types.
     */
    GCP,

    /**
     * Docker registry setting variable types.
     */
    DOCKER,

    /**
     * Physical data center setting variable types.
     */
    PHYSICAL_DATA_CENTER,

    /**
     * Kubernetes setting variable types.
     */
    KUBERNETES,

    /**
     * Kubernetes setting variable types.
     */
    NEXUS,
  }
}
