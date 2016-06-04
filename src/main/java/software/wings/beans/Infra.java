package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

import java.util.Objects;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/1/16.
 */
@Entity(value = "infras", noClassnameStored = true)
public class Infra extends Base {
  private InfraType infraType;
  private String envId;

  public InfraType getInfraType() {
    return infraType;
  }

  public void setInfraType(InfraType infraType) {
    this.infraType = infraType;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#hashCode()
   */
  @Override
  public int hashCode() {
    return 31 * super.hashCode() + Objects.hash(infraType, envId);
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#equals(java.lang.Object)
   */
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
    final Infra other = (Infra) obj;
    return Objects.equals(this.infraType, other.infraType) && Objects.equals(this.envId, other.envId);
  }

  /**
   * The Enum InfraType.
   */
  public static enum InfraType { STATIC, AWS, AZURE, CONTAINER }

  /**
   * The Class InfraBuilder.
   */
  public static final class InfraBuilder {
    private InfraType infraType;
    private String envId;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private InfraBuilder() {}

    /**
     * An infra.
     *
     * @return the infra builder
     */
    public static InfraBuilder anInfra() {
      return new InfraBuilder();
    }

    /**
     * With infra type.
     *
     * @param infraType the infra type
     * @return the infra builder
     */
    public InfraBuilder withInfraType(InfraType infraType) {
      this.infraType = infraType;
      return this;
    }

    /**
     * With env id.
     *
     * @param envId the env id
     * @return the infra builder
     */
    public InfraBuilder withEnvId(String envId) {
      this.envId = envId;
      return this;
    }

    /**
     * With uuid.
     *
     * @param uuid the uuid
     * @return the infra builder
     */
    public InfraBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id.
     *
     * @param appId the app id
     * @return the infra builder
     */
    public InfraBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by.
     *
     * @param createdBy the created by
     * @return the infra builder
     */
    public InfraBuilder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at.
     *
     * @param createdAt the created at
     * @return the infra builder
     */
    public InfraBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by.
     *
     * @param lastUpdatedBy the last updated by
     * @return the infra builder
     */
    public InfraBuilder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at.
     *
     * @param lastUpdatedAt the last updated at
     * @return the infra builder
     */
    public InfraBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active.
     *
     * @param active the active
     * @return the infra builder
     */
    public InfraBuilder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But.
     *
     * @return the infra builder
     */
    public InfraBuilder but() {
      return anInfra()
          .withInfraType(infraType)
          .withEnvId(envId)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Builds the.
     *
     * @return the infra
     */
    public Infra build() {
      Infra infra = new Infra();
      infra.setInfraType(infraType);
      infra.setEnvId(envId);
      infra.setUuid(uuid);
      infra.setAppId(appId);
      infra.setCreatedBy(createdBy);
      infra.setCreatedAt(createdAt);
      infra.setLastUpdatedBy(lastUpdatedBy);
      infra.setLastUpdatedAt(lastUpdatedAt);
      infra.setActive(active);
      return infra;
    }
  }
}
