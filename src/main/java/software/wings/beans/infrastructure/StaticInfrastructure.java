package software.wings.beans.infrastructure;

import software.wings.beans.User;

/**
 * Created by anubhaw on 9/13/16.
 */
public class StaticInfrastructure extends Infrastructure {
  /**
   * Instantiates a new Static infrastructure.
   */
  public StaticInfrastructure() {
    super(InfrastructureType.STATIC);
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private String name;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A static infrastructure builder.
     *
     * @return the builder
     */
    public static Builder aStaticInfrastructure() {
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
      return aStaticInfrastructure()
          .withName(name)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    /**
     * Build static infrastructure.
     *
     * @return the static infrastructure
     */
    public StaticInfrastructure build() {
      StaticInfrastructure staticInfrastructure = new StaticInfrastructure();
      staticInfrastructure.setName(name);
      staticInfrastructure.setUuid(uuid);
      staticInfrastructure.setAppId(appId);
      staticInfrastructure.setCreatedBy(createdBy);
      staticInfrastructure.setCreatedAt(createdAt);
      staticInfrastructure.setLastUpdatedBy(lastUpdatedBy);
      staticInfrastructure.setLastUpdatedAt(lastUpdatedAt);
      staticInfrastructure.setActive(active);
      return staticInfrastructure;
    }
  }
}
