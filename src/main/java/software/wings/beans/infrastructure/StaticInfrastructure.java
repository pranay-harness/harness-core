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

  public static final class Builder {
    private String name;
    private HostUsage hostUsage;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    public static Builder aStaticInfrastructure() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withHostUsage(HostUsage hostUsage) {
      this.hostUsage = hostUsage;
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

    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    public Builder but() {
      return aStaticInfrastructure()
          .withName(name)
          .withHostUsage(hostUsage)
          .withUuid(uuid)
          .withAppId(appId)
          .withCreatedBy(createdBy)
          .withCreatedAt(createdAt)
          .withLastUpdatedBy(lastUpdatedBy)
          .withLastUpdatedAt(lastUpdatedAt)
          .withActive(active);
    }

    public StaticInfrastructure build() {
      StaticInfrastructure staticInfrastructure = new StaticInfrastructure();
      staticInfrastructure.setName(name);
      staticInfrastructure.setHostUsage(hostUsage);
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
