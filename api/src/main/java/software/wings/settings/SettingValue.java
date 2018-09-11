package software.wings.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ro.fortsoft.pf4j.ExtensionPoint;
import software.wings.utils.WingsReflectionUtils;
import software.wings.yaml.BaseEntityYaml;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by anubhaw on 5/16/16.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = As.EXISTING_PROPERTY)
public abstract class SettingValue implements ExtensionPoint {
  private String type;

  private boolean decrypted;

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
   * Gets setting type.
   *
   * @return the setting type
   */
  @SchemaIgnore
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.valueOf(type);
  }

  /**
   * Sets setting type.
   *
   * @param type the type
   */
  public void setSettingType(SettingVariableTypes type) {
    //
  }

  /**
   * Gets encrypted fields.
   *
   * @return the encrypted fields
   */
  @SchemaIgnore
  public boolean isDecrypted() {
    return decrypted;
  }

  public void setDecrypted(boolean decrypted) {
    this.decrypted = decrypted;
  }

  @SchemaIgnore
  @JsonIgnore
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
     * Datadog setting variable type.
     */
    DATA_DOG("Datadog"),

    /**
     * Generic APM setting variable types.
     */
    APM_VERIFICATION("APM Verification"),
    /**
     * Bugsnag setting variable type.
     */
    BUG_SNAG("Bugsnag"),

    /**
     * Generic Log setting variable types.
     */
    LOG_VERIFICATION("Log Verification"),

    /**
     * App dynamics setting variable types.
     */
    APP_DYNAMICS("AppDynamics"),

    /**
     * New relic setting variable types.
     */
    NEW_RELIC("NewRelic"),

    /**
     * Dyna trace variable types.
     */
    DYNA_TRACE("Dynatrace"),

    /**
     * Dyna trace variable types.
     */
    CLOUD_WATCH("Cloudwatch"),

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
     * GCS setting variable types.
     */
    GCS("Google Cloud Storage"),

    /**
     * GCP setting variable types.
     */
    GCP("Google Cloud Platform"),

    /**
     * Azure setting variable types.
     */
    AZURE("Microsoft Azure"),

    /**
     * Pivotal Cloud Foundry
     */
    PCF("Pivotal Cloud Foundry"),

    /**
     * Direct connection setting variable types.
     */
    @Deprecated DIRECT("Direct Kubernetes"),

    /**
     * Kubernetes Cluster setting variable types.
     */
    KUBERNETES_CLUSTER("Kubernetes Cluster"),

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
     * Azure Container Registry setting variable types.
     */
    ACR("Azure Container Registry (ACR)"),

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

    /**
     * Git setting variable types.
     */
    GIT("GIT"),
    /**
     * Ssh session config setting variable types.
     */
    SSH_SESSION_CONFIG,

    /**
     * Service variable setting variable types.
     */
    SERVICE_VARIABLE,

    /**
     * Config file setting variable types.
     */
    CONFIG_FILE,

    /**
     * Kms setting variable types.
     */
    KMS,

    SECRET_TEXT,

    YAML_GIT_SYNC,

    VAULT,

    WINRM_CONNECTION_ATTRIBUTES("WinRm Connection Attributes"),

    WINRM_SESSION_CONFIG,

    PROMETHEUS("Prometheus"),

    INFRASTRUCTURE_MAPPING;

    private String displayName;

    SettingVariableTypes() {
      this.displayName = name();
    }
    SettingVariableTypes(String displayName) {
      this.displayName = displayName;
    }

    /**
     * Gets display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
      return displayName;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public abstract static class Yaml extends BaseEntityYaml {
    private UsageRestrictions usageRestrictions;

    public Yaml(String type, String harnessApiVersion, UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion);
      this.usageRestrictions = usageRestrictions;
    }
  }
}
