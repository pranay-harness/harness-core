package software.wings.beans;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.ServiceVariable.Builder.aServiceVariable;

import com.google.common.base.MoreObjects;

import com.github.reinert.jjschema.SchemaIgnore;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.utils.validation.Create;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
@Entity(value = "serviceVariables", noClassnameStored = true)
@Indexes(@Index(fields =
    {
      @Field("entityId")
      , @Field("templateId"), @Field("overrideType"), @Field("instances"), @Field("expression"), @Field("type"),
          @Field("name")
    },
    options = @IndexOptions(unique = true, name = "serviceVariableUniqueIdx")))
public class ServiceVariable extends Base implements Encryptable {
  /**
   * The constant DEFAULT_TEMPLATE_ID.
   */
  public static final String DEFAULT_TEMPLATE_ID = "__TEMPLATE_ID";

  private String templateId = DEFAULT_TEMPLATE_ID;

  @NotEmpty(groups = {Create.class}) private String envId;

  @NotNull(groups = {Create.class}) private EntityType entityType;

  @NotEmpty(groups = {Create.class}) private String entityId;

  private String parentServiceVariableId;

  @Transient private ServiceVariable overriddenServiceVariable;

  private OverrideType overrideType;

  private List<String> instances;
  private String expression;

  @SchemaIgnore private String accountId;

  private String name;

  @Encrypted private char[] value;

  private Type type;

  /**
   * Getter for property 'templateId'.
   *
   * @return Value for property 'templateId'.
   */
  public String getTemplateId() {
    return templateId;
  }

  /**
   * Setter for property 'templateId'.
   *
   * @param templateId Value to set for property 'templateId'.
   */
  public void setTemplateId(String templateId) {
    if (isBlank(templateId)) {
      templateId = DEFAULT_TEMPLATE_ID;
    }
    this.templateId = templateId;
  }

  /**
   * Getter for property 'envId'.
   *
   * @return Value for property 'envId'.
   */
  public String getEnvId() {
    return envId;
  }

  /**
   * Setter for property 'envId'.
   *
   * @param envId Value to set for property 'envId'.
   */
  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /**
   * Getter for property 'entityType'.
   *
   * @return Value for property 'entityType'.
   */
  public EntityType getEntityType() {
    return entityType;
  }

  /**
   * Setter for property 'entityType'.
   *
   * @param entityType Value to set for property 'entityType'.
   */
  public void setEntityType(EntityType entityType) {
    this.entityType = entityType;
  }

  /**
   * Getter for property 'entityId'.
   *
   * @return Value for property 'entityId'.
   */
  public String getEntityId() {
    return entityId;
  }

  /**
   * Setter for property 'entityId'.
   *
   * @param entityId Value to set for property 'entityId'.
   */
  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  /**
   * Getter for property 'value'.
   *
   * @return Value for property 'value'.
   */
  public char[] getValue() {
    return value;
  }

  /**
   * Setter for property 'value'.
   *
   * @param value Value to set for property 'value'.
   */
  public void setValue(char[] value) {
    this.value = value;
  }

  /**
   * Getter for property 'type'.
   *
   * @return Value for property 'type'.
   */
  public Type getType() {
    return type;
  }

  /**
   * Setter for property 'type'.
   *
   * @param type Value to set for property 'type'.
   */
  public void setType(Type type) {
    this.type = type;
  }

  /**
   * Getter for property 'name'.
   *
   * @return Value for property 'name'.
   */
  public String getName() {
    return name;
  }

  /**
   * Setter for property 'name'.
   *
   * @param name Value to set for property 'name'.
   */
  public void setName(String name) {
    this.name = name;
  }

  public String getParentServiceVariableId() {
    return parentServiceVariableId;
  }

  public void setParentServiceVariableId(String parentServiceVariableId) {
    this.parentServiceVariableId = parentServiceVariableId;
  }

  public ServiceVariable getOverriddenServiceVariable() {
    return overriddenServiceVariable;
  }

  public void setOverriddenServiceVariable(ServiceVariable overriddenServiceVariable) {
    this.overriddenServiceVariable = overriddenServiceVariable;
  }

  @Override
  @SchemaIgnore
  public String getAccountId() {
    return this.getAppId();
  }

  @Override
  @SchemaIgnore
  public void setAccountId(String accountId) {}

  /**
   * Getter for property 'overrideType'.
   *
   * @return Value for property 'overrideType'.
   */
  public OverrideType getOverrideType() {
    return overrideType;
  }

  /**
   * Setter for property 'overrideType'.
   *
   * @param overrideType Value to set for property 'overrideType'.
   */
  public void setOverrideType(OverrideType overrideType) {
    this.overrideType = overrideType;
  }

  /**
   * Getter for property 'instances'.
   *
   * @return Value for property 'instances'.
   */
  public List<String> getInstances() {
    return instances;
  }

  /**
   * Setter for property 'instances'.
   *
   * @param instances Value to set for property 'instances'.
   */
  public void setInstances(List<String> instances) {
    this.instances = instances;
  }

  /**
   * Getter for property 'expression'.
   *
   * @return Value for property 'expression'.
   */
  public String getExpression() {
    return expression;
  }

  /**
   * Setter for property 'expression'.
   *
   * @param expression Value to set for property 'expression'.
   */
  public void setExpression(String expression) {
    this.expression = expression;
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(templateId, envId, entityType, entityId, parentServiceVariableId, overriddenServiceVariable,
              overrideType, instances, expression, name, value, type);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    if (!super.equals(obj)) {
      return false;
    }
    final ServiceVariable other = (ServiceVariable) obj;
    return Objects.equals(this.templateId, other.templateId) && Objects.equals(this.envId, other.envId)
        && Objects.equals(this.entityType, other.entityType) && Objects.equals(this.entityId, other.entityId)
        && Objects.equals(this.parentServiceVariableId, other.parentServiceVariableId)
        && Objects.equals(this.overriddenServiceVariable, other.overriddenServiceVariable)
        && Objects.equals(this.overrideType, other.overrideType) && Objects.equals(this.instances, other.instances)
        && Objects.equals(this.expression, other.expression) && Objects.equals(this.name, other.name)
        && Arrays.equals(this.value, other.value) && Objects.equals(this.type, other.type);
  }

  @Override
  public String toString() {
    if (type.equals(Type.ENCRYPTED_TEXT)) {
      value = "******".toCharArray();
    }
    return MoreObjects.toStringHelper(this)
        .add("templateId", templateId)
        .add("envId", envId)
        .add("entityType", entityType)
        .add("entityId", entityId)
        .add("parentServiceVariableId", parentServiceVariableId)
        .add("overriddenServiceVariable", overriddenServiceVariable)
        .add("overrideType", overrideType)
        .add("instances", instances)
        .add("expression", expression)
        .add("name", name)
        .add("value", value)
        .add("type", type)
        .add("accountId", accountId)
        .toString();
  }

  /**
   * The enum Type.
   */
  public enum Type {
    /**
     * Text type.
     */
    TEXT, /**
           * Lb type.
           */
    LB(true), /**
               * Encrypted text type.
               */
    ENCRYPTED_TEXT;

    private boolean settingAttribute;

    Type() {}

    Type(boolean settingAttribute) {
      this.settingAttribute = settingAttribute;
    }

    /**
     * Is setting attribute boolean.
     *
     * @return the boolean
     */
    public boolean isSettingAttribute() {
      return settingAttribute;
    }
  }

  public enum OverrideType {
    /**
     * All  override type.
     */
    ALL, /**
          * Instances override type.
          */
    INSTANCES, /**
                * Custom override type.
                */
    CUSTOM
  }

  public ServiceVariable clone() {
    return aServiceVariable()
        .withAccountId(getAccountId())
        .withAppId(getAppId())
        .withEnvId(getEnvId())
        .withEntityId(getUuid())
        .withEntityType(getEntityType())
        .withTemplateId(getTemplateId())
        .withName(getName())
        .withValue(getValue())
        .withType(getType())
        .build();
  }

  public static final class Builder {
    private String templateId = DEFAULT_TEMPLATE_ID;
    private String envId;
    private EntityType entityType;
    private String uuid;
    private String appId;
    private String entityId;
    private EmbeddedUser createdBy;
    private String parentServiceVariableId;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private ServiceVariable overriddenServiceVariable;
    private long lastUpdatedAt;
    private OverrideType overrideType;
    private List<String> instances;
    private String expression;
    private String name;
    private char[] value;
    private Type type;
    private String accountId;

    private Builder() {}

    public static Builder aServiceVariable() {
      return new Builder();
    }

    public Builder withTemplateId(String templateId) {
      this.templateId = templateId;
      return this;
    }

    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    public Builder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    public Builder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withParentServiceVariableId(String parentServiceVariableId) {
      this.parentServiceVariableId = parentServiceVariableId;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withOverriddenServiceVariable(ServiceVariable overriddenServiceVariable) {
      this.overriddenServiceVariable = overriddenServiceVariable;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withOverrideType(OverrideType overrideType) {
      this.overrideType = overrideType;
      return this;
    }

    public Builder withInstances(List<String> instances) {
      this.instances = instances;
      return this;
    }

    public Builder withExpression(String expression) {
      this.expression = expression;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withValue(char[] value) {
      this.value = value;
      return this;
    }

    public Builder withType(Type type) {
      this.type = type;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder but() {
      return aServiceVariable()
          .withTemplateId(templateId)
          .withEnvId(envId)
          .withEntityType(entityType)
          .withUuid(uuid)
          .withAppId(appId)
          .withEntityId(entityId)
          .withCreatedBy(createdBy)
          .withParentServiceVariableId(parentServiceVariableId)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withOverriddenServiceVariable(overriddenServiceVariable)
          .withLastUpdatedAt(lastUpdatedAt)
          .withOverrideType(overrideType)
          .withInstances(instances)
          .withExpression(expression)
          .withName(name)
          .withValue(value)
          .withType(type)
          .withAccountId(accountId);
    }

    public ServiceVariable build() {
      ServiceVariable serviceVariable = new ServiceVariable();
      serviceVariable.setTemplateId(templateId);
      serviceVariable.setEnvId(envId);
      serviceVariable.setEntityType(entityType);
      serviceVariable.setUuid(uuid);
      serviceVariable.setAppId(appId);
      serviceVariable.setEntityId(entityId);
      serviceVariable.setCreatedBy(createdBy);
      serviceVariable.setParentServiceVariableId(parentServiceVariableId);
      serviceVariable.setCreatedAt(createdAt);
      serviceVariable.setLastUpdatedBy(lastUpdatedBy);
      serviceVariable.setOverriddenServiceVariable(overriddenServiceVariable);
      serviceVariable.setLastUpdatedAt(lastUpdatedAt);
      serviceVariable.setOverrideType(overrideType);
      serviceVariable.setInstances(instances);
      serviceVariable.setExpression(expression);
      serviceVariable.setName(name);
      serviceVariable.setValue(value);
      serviceVariable.setType(type);
      serviceVariable.setAccountId(accountId);
      return serviceVariable;
    }
  }
}
