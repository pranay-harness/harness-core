package software.wings.app;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by anubhaw on 11/9/16.
 */
public class SchedulerConfig {
  @JsonProperty(defaultValue = "com.novemberain.quartz.mongodb.DynamicMongoDBJobStore")
  private String jobstoreclass = "com.novemberain.quartz.mongodb.DynamicMongoDBJobStore";

  @JsonProperty(defaultValue = "1") private String threadCount = "1";
  @JsonProperty(defaultValue = "10000") private String idleWaitTime = "10000";
  @JsonProperty(defaultValue = "true") private String autoStart = "true";
  @JsonProperty(defaultValue = "automation_scheduler") private String schedulerName = "automation_scheduler";
  @JsonProperty(defaultValue = "automation") private String instanceId = "automation";
  @JsonProperty(defaultValue = "quartz") private String tablePrefix = "quartz";
  @JsonProperty(defaultValue = "true") private boolean isClustered;
  @JsonProperty(defaultValue = "20000") private String mongoOptionWriteConcernTimeoutMillis = "20000";

  public String getJobstoreclass() {
    return jobstoreclass;
  }

  public void setJobstoreclass(String jobstoreclass) {
    this.jobstoreclass = jobstoreclass;
  }

  public String getThreadCount() {
    return threadCount;
  }

  public void setThreadCount(String threadCount) {
    this.threadCount = threadCount;
  }

  public String getIdleWaitTime() {
    return idleWaitTime;
  }

  public void setIdleWaitTime(String idleWaitTime) {
    this.idleWaitTime = idleWaitTime;
  }

  public String getAutoStart() {
    return autoStart;
  }

  public void setAutoStart(String autoStart) {
    this.autoStart = autoStart;
  }

  public String getSchedulerName() {
    return schedulerName;
  }

  public void setSchedulerName(String schedulerName) {
    this.schedulerName = schedulerName;
  }

  public String getInstanceId() {
    return instanceId;
  }

  public void setInstanceId(String instanceId) {
    this.instanceId = instanceId;
  }

  public String getTablePrefix() {
    return tablePrefix;
  }

  public void setTablePrefix(String tablePrefix) {
    this.tablePrefix = tablePrefix;
  }

  public boolean isClustered() {
    return isClustered;
  }

  public void setClustered(boolean clustered) {
    isClustered = clustered;
  }

  public String getMongoOptionWriteConcernTimeoutMillis() {
    return mongoOptionWriteConcernTimeoutMillis;
  }

  public void setMongoOptionWriteConcernTimeoutMillis(String mongoOptionWriteConcernTimeoutMillis) {
    this.mongoOptionWriteConcernTimeoutMillis = mongoOptionWriteConcernTimeoutMillis;
  }
}
