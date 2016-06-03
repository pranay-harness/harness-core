package software.wings.beans;

import com.google.common.base.MoreObjects;

import org.mongodb.morphia.annotations.Entity;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
@Entity(value = "commandLogs", noClassnameStored = true)
public class Log extends Base {
  private String activityId;
  private String hostName;
  private String serviceTemplateId;
  private String logLine;

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public String getHostName() {
    return hostName;
  }

  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  public String getServiceTemplateId() {
    return serviceTemplateId;
  }

  public void setServiceTemplateId(String serviceTemplateId) {
    this.serviceTemplateId = serviceTemplateId;
  }

  public String getLogLine() {
    return logLine;
  }

  public void setLogLine(String logLine) {
    this.logLine = logLine;
  }

  /* (non-Javadoc)
   * @see software.wings.beans.Base#toString()
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("activityId", activityId)
        .add("hostName", hostName)
        .add("serviceTemplateId", serviceTemplateId)
        .add("logLine", logLine)
        .toString();
  }

  /**
   * The Class Builder.
   */
  public static final class Builder {
    private String activityId;
    private String hostName;
    private String serviceTemplateId;
    private String logLine;
    private String uuid;
    private String appId;
    private User createdBy;
    private long createdAt;
    private User lastUpdatedBy;
    private long lastUpdatedAt;
    private boolean active = true;

    private Builder() {}

    /**
     * A log.
     *
     * @return the builder
     */
    public static Builder aLog() {
      return new Builder();
    }

    /**
     * With activity id.
     *
     * @param activityId the activity id
     * @return the builder
     */
    public Builder withActivityId(String activityId) {
      this.activityId = activityId;
      return this;
    }

    /**
     * With host name.
     *
     * @param hostName the host name
     * @return the builder
     */
    public Builder withHostName(String hostName) {
      this.hostName = hostName;
      return this;
    }

    /**
     * With service template id.
     *
     * @param serviceTemplateId the service template id
     * @return the builder
     */
    public Builder withServiceTemplateId(String serviceTemplateId) {
      this.serviceTemplateId = serviceTemplateId;
      return this;
    }

    /**
     * With log line.
     *
     * @param logLine the log line
     * @return the builder
     */
    public Builder withLogLine(String logLine) {
      this.logLine = logLine;
      return this;
    }

    /**
     * With uuid.
     *
     * @param uuid the uuid
     * @return the builder
     */
    public Builder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    /**
     * With app id.
     *
     * @param appId the app id
     * @return the builder
     */
    public Builder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    /**
     * With created by.
     *
     * @param createdBy the created by
     * @return the builder
     */
    public Builder withCreatedBy(User createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    /**
     * With created at.
     *
     * @param createdAt the created at
     * @return the builder
     */
    public Builder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * With last updated by.
     *
     * @param lastUpdatedBy the last updated by
     * @return the builder
     */
    public Builder withLastUpdatedBy(User lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    /**
     * With last updated at.
     *
     * @param lastUpdatedAt the last updated at
     * @return the builder
     */
    public Builder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    /**
     * With active.
     *
     * @param active the active
     * @return the builder
     */
    public Builder withActive(boolean active) {
      this.active = active;
      return this;
    }

    /**
     * But.
     *
     * @return the builder
     */
    public Builder but() {
      return aLog()
          .withActivityId(activityId)
          .withHostName(hostName)
          .withServiceTemplateId(serviceTemplateId)
          .withLogLine(logLine)
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
     * @return the log
     */
    public Log build() {
      Log log = new Log();
      log.setActivityId(activityId);
      log.setHostName(hostName);
      log.setServiceTemplateId(serviceTemplateId);
      log.setLogLine(logLine);
      log.setUuid(uuid);
      log.setAppId(appId);
      log.setCreatedBy(createdBy);
      log.setCreatedAt(createdAt);
      log.setLastUpdatedBy(lastUpdatedBy);
      log.setLastUpdatedAt(lastUpdatedAt);
      log.setActive(active);
      return log;
    }
  }
}
