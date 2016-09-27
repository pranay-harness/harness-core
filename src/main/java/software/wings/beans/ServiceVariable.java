package software.wings.beans;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.common.base.MoreObjects;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.utils.validation.Create;

import java.util.Objects;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 9/14/16.
 */
@Entity(value = "serviceVariables", noClassnameStored = true)
@Indexes(@Index(
    fields = { @Field("entityId")
               , @Field("templateId"), @Field("type") }, options = @IndexOptions(unique = true)))
public class ServiceVariable extends Base {
  /**
   * The constant DEFAULT_TEMPLATE_ID.
   */
  public static final String DEFAULT_TEMPLATE_ID = "__TEMPLATE_ID";

  private String templateId = DEFAULT_TEMPLATE_ID;

  @NotEmpty(groups = {Create.class}) private String envId;

  @NotNull(groups = {Create.class}) private EntityType entityType;

  @NotEmpty(groups = {Create.class}) private String entityId;

  private String name;
  private String value;

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
  public String getValue() {
    return value;
  }

  /**
   * Setter for property 'value'.
   *
   * @param value Value to set for property 'value'.
   */
  public void setValue(String value) {
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

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(templateId, envId, entityType, entityId, name, value, type);
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
        && Objects.equals(this.name, other.name) && Objects.equals(this.value, other.value)
        && Objects.equals(this.type, other.type);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("templateId", templateId)
        .add("envId", envId)
        .add("entityType", entityType)
        .add("entityId", entityId)
        .add("name", name)
        .add("value", value)
        .add("type", type)
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
    LB(true);

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

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String templateId;
    private String envId;
    private EntityType entityType;
    private String entityId;
    private String name;
    private String value;
    private Type type;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A service variable builder.
     *
     * @return the builder
     */
    public static Builder aServiceVariable() {
      return new Builder();
    }

    /**
     * With template id builder.
     *
     * @param templateId the template id
     * @return the builder
     */
    public Builder withTemplateId(String templateId) {
      this.templateId = templateId;
      return this;
    }

    /**
     * With env id builder.
     *
     * @param envId the env id
     * @return the builder
     */
    public Builder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With entity type builder.
     *
     * @param entityType the entity type
     * @return the builder
     */
    public Builder withEntityType(EntityType entityType) {
      this.entityType = entityType;
      return this;
    }

    /**
     * With entity id builder.
     *
     * @param entityId the entity id
     * @return the builder
     */
    public Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }

    /**
     * With name builder.
     *
     * @param name the name
     * @return the builder
     */
    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    /**
     * With value builder.
     *
     * @param value the value
     * @return the builder
     */
    public Builder withValue(String value) {
      this.value = value;
      return this;
    }

    /**
     * With type builder.
     *
     * @param type the type
     * @return the builder
     */
    public Builder withType(Type type) {
      this.type = type;
      return this;
    }

    /**
     * With uuid builder.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id builder.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by builder.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at builder.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by builder.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at builder.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active builder.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aServiceVariable()
          .withTemplateId(templateId)
          .withEnvId(envId)
          .withEntityType(entityType)
          .withEntityId(entityId)
          .withName(name)
          .withValue(value)
          .withType(type)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build service variable.
     *
     * @return the service variable
     */
    public ServiceVariable build() {
      ServiceVariable serviceVariable = new ServiceVariable();
      serviceVariable.setTemplateId(templateId);
      serviceVariable.setEnvId(envId);
      serviceVariable.setEntityType(entityType);
      serviceVariable.setEntityId(entityId);
      serviceVariable.setName(name);
      serviceVariable.setValue(value);
      serviceVariable.setType(type);
      serviceVariable.setUuid(uuid);
      serviceVariable.setAppId(appId);
      serviceVariable.setCreatedBy(createdBy);
      serviceVariable.setCreatedAt(createdAt);
      serviceVariable.setLastUpdatedBy(lastUpdatedBy);
      serviceVariable.setLastUpdatedAt(lastUpdatedAt);
      serviceVariable.setActive(active);
      return serviceVariable;
    }
  }
}
