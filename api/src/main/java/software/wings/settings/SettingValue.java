package software.wings.settings;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.github.reinert.jjschema.SchemaIgnore;
import ro.fortsoft.pf4j.ExtensionPoint;
import software.wings.utils.WingsReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;

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

  @SchemaIgnore
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.valueOf(type);
  }

  public void setSettingType(SettingVariableTypes type) {
    //
  }

  public List<Field> getEncryptedFields() {
    return WingsReflectionUtils.getEncryptedFields(this.getClass());
  }

  /**
   * The Enum SettingVariableTypes.
   */
  public enum SettingVariableTypes {
    /**
     * Host connection attributes setting variable types.
     */
    HOST_CONNECTION_ATTRIBUTES("Host Connection Attributes"),

    /**
     * Bastion host connection attributes setting variable types.
     */
    BASTION_HOST_CONNECTION_ATTRIBUTES("Bastion Host Connection Attributes"),

    /**
     * Smtp setting variable types.
     */
    SMTP("SMTP"),

    /**
     * Jenkins setting variable types.
     */
    JENKINS("Jenkins"),

    /**
     * Bamboo setting variable types.
     */
    BAMBOO("Bamboo"),

    /**
     * String setting variable types.
     */
    STRING,

    /**
     * Splunk setting variable types.
     */
    SPLUNK("Splunk"),

    /**
     * Elk setting variable types.
     */
    ELK("ELK"),

    /**
     * Logz setting variable types.
     */
    LOGZ("LOGZ"),

    /**
     * SUMO setting variable types.
     */
    SUMO("SumoLogic"),

    /**
     * App dynamics setting variable types.
     */
    APP_DYNAMICS("AppDynamics"),

    /**
     * New relic setting variable types.
     */
    NEW_RELIC("NewRelic"),

    /**
     * Elastic Load Balancer Settings
     */
    ELB("Elastic Classic Load Balancer"),

    /**
     * Slack setting variable types.
     */
    SLACK("SLACK"),

    /**
     * AWS setting variable types.
     */
    AWS("Amazon Web Services"),

    /**
     * GCP setting variable types.
     */
    GCP("Google Cloud Platform"),

    /**
     * Direct connection setting variable types.
     */
    DIRECT("Direct Kubernetes"),

    /**
     * Docker registry setting variable types.
     */
    DOCKER("Docker Registry"),

    /**
     * AWS ECR registry setting variable types.
     */
    ECR("Amazon EC2 Container Registry (ECR)"),

    /**
     * Google Container Registry setting variable types.
     */
    GCR("Google Container Registry (GCR)"),

    /**
     * Physical data center setting variable types.
     */
    PHYSICAL_DATA_CENTER("Physical Data Center"),

    /**
     * Kubernetes setting variable types.
     */
    KUBERNETES("Kubernetes"),

    /**
     * Nexus setting variable types.
     */
    NEXUS("Nexus"),

    /**
     * Artifactory setting variable types
     */
    ARTIFACTORY("Artifactory"),

    /**
     * Amazon S3 setting variable types
     */
    AMAZON_S3("AmazonS3"),

    SSH_SESSION_CONFIG,

    SERVICE_VARIABLE,

    CONFIG_FILE,

    KMS;

    private String displayName;

    SettingVariableTypes() {
      this.displayName = name();
    }
    SettingVariableTypes(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }
}
