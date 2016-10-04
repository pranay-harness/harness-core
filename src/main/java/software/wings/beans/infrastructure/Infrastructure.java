package software.wings.beans.infrastructure;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Base;
import software.wings.beans.InfrastructureMappingRule;
import software.wings.beans.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by anubhaw on 4/1/16.
 */
@Entity(value = "infrastructure")
@Indexes(@Index(fields = { @Field("name") }, options = @IndexOptions(unique = true)))
public class Infrastructure extends Base {
  private String name;
  private InfrastructureType type;
  private String infrastructureConfigId;
  private List<InfrastructureMappingRule> infrastructureMappingRules = new ArrayList<>();
  @Transient private HostUsage hostUsage;

  /**
   * Instantiates a new Infrastructure.
   */
  public Infrastructure() {
    setAppId(GLOBAL_APP_ID);
  }

  /**
   * Gets infra type.
   *
   * @return the infra type
   */
  public InfrastructureType getType() {
    return type;
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets name.
   *
   * @param name the name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets host usage.
   *
   * @return the host usage
   */
  public HostUsage getHostUsage() {
    return hostUsage;
  }

  /**
   * Sets host usage.
   *
   * @param hostUsage the host usage
   */
  public void setHostUsage(HostUsage hostUsage) {
    this.hostUsage = hostUsage;
  }

  /**
   * Gets infrastructure mapping rules.
   *
   * @return the infrastructure mapping rules
   */
  public List<InfrastructureMappingRule> getInfrastructureMappingRules() {
    return infrastructureMappingRules;
  }

  /**
   * Sets infrastructure mapping rules.
   *
   * @param infrastructureMappingRules the infrastructure mapping rules
   */
  public void setInfrastructureMappingRules(List<InfrastructureMappingRule> infrastructureMappingRules) {
    this.infrastructureMappingRules = infrastructureMappingRules;
  }

  /**
   * Gets infrastructure config id.
   *
   * @return the infrastructure config id
   */
  public String getInfrastructureConfigId() {
    return infrastructureConfigId;
  }

  /**
   * Sets infrastructure config id.
   *
   * @param infrastructureConfigId the infrastructure config id
   */
  public void setInfrastructureConfigId(String infrastructureConfigId) {
    this.infrastructureConfigId = infrastructureConfigId;
  }

  /**
   * Sets type.
   *
   * @param type the type
   */
  public void setType(InfrastructureType type) {
    this.type = type;
  }

  /**
   * The Enum InfrastructureType.
   */
  public enum InfrastructureType {
    /**
     * Static infra type.
     */
    STATIC, /**
             * Aws infra type.
             */
    AWS, /**
          * Azure infra type.
          */
    AZURE, /**
            * Container infra type.
            */
    CONTAINER
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode()
        + Objects.hash(name, type, infrastructureMappingRules, hostUsage, infrastructureConfigId);
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
    final Infrastructure other = (Infrastructure) obj;
    return Objects.equals(this.name, other.name) && Objects.equals(this.type, other.type)
        && Objects.equals(this.infrastructureMappingRules, other.infrastructureMappingRules)
        && Objects.equals(this.hostUsage, other.hostUsage)
        && Objects.equals(this.infrastructureConfigId, other.infrastructureConfigId);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("name", name)
        .add("type", type)
        .add("infrastructureMappingRules", infrastructureMappingRules)
        .add("hostUsage", hostUsage)
        .add("infrastructureConfigId", infrastructureConfigId)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private InfrastructureType type;
    private List<InfrastructureMappingRule> infrastructureMappingRules = new ArrayList<>();
    private HostUsage hostUsage;
    private String infrastructureConfigId;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * An infrastructure builder.
     *
     * @return the builder
     */
    public static Builder anInfrastructure() {
      return new Builder();
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
     * With type builder.
     *
     * @param type the type
     * @return the builder
     */
    public Builder withType(InfrastructureType type) {
      this.type = type;
      return this;
    }

    /**
     * With infrastructure mapping rules builder.
     *
     * @param infrastructureMappingRules the infrastructure mapping rules
     * @return the builder
     */
    public Builder withInfrastructureMappingRules(List<InfrastructureMappingRule> infrastructureMappingRules) {
      this.infrastructureMappingRules = infrastructureMappingRules;
      return this;
    }

    /**
     * With host usage builder.
     *
     * @param hostUsage the host usage
     * @return the builder
     */
    public Builder withHostUsage(HostUsage hostUsage) {
      this.hostUsage = hostUsage;
      return this;
    }

    /**
     * With infrastructure config id builder.
     *
     * @param infrastructureConfigId the infrastructure config id
     * @return the builder
     */
    public Builder withInfrastructureConfigId(String infrastructureConfigId) {
      this.infrastructureConfigId = infrastructureConfigId;
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
      return anInfrastructure()
          .withName(name)
          .withType(type)
          .withInfrastructureMappingRules(infrastructureMappingRules)
          .withHostUsage(hostUsage)
          .withInfrastructureConfigId(infrastructureConfigId)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build infrastructure.
     *
     * @return the infrastructure
     */
    public Infrastructure build() {
      Infrastructure infrastructure = new Infrastructure();
      infrastructure.setName(name);
      infrastructure.setType(type);
      infrastructure.setInfrastructureMappingRules(infrastructureMappingRules);
      infrastructure.setHostUsage(hostUsage);
      infrastructure.setInfrastructureConfigId(infrastructureConfigId);
      infrastructure.setUuid(uuid);
      infrastructure.setAppId(appId);
      infrastructure.setCreatedBy(createdBy);
      infrastructure.setCreatedAt(createdAt);
      infrastructure.setLastUpdatedBy(lastUpdatedBy);
      infrastructure.setLastUpdatedAt(lastUpdatedAt);
      infrastructure.setActive(active);
      return infrastructure;
    }
  }
}
