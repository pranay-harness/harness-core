package software.wings.app;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by anubhaw on 11/9/16.
 */
public class SchedulerConfig {
  @JsonProperty(defaultValue = "com.novemberain.quartz.mongodb.DynamicMongoDBJobStore")
  private String jobstoreclass = "com.novemberain.quartz.mongodb.DynamicMongoDBJobStore";
  @JsonProperty(defaultValue = "wings") private String jobstoreDbName = "wings";
  @JsonProperty(defaultValue = "mongodb://localhost:27017") private String jobstoreDbUrl = "mongodb://localhost:27017";
  @JsonProperty(defaultValue = "2") private String threadCount = "2";
  @JsonProperty(defaultValue = "10000") private String idleWaitTime = "10000";

  public String getJobstoreclass() {
    return jobstoreclass;
  }

  public void setJobstoreclass(String jobstoreclass) {
    this.jobstoreclass = jobstoreclass;
  }

  public String getJobstoreDbName() {
    return jobstoreDbName;
  }

  public void setJobstoreDbName(String jobstoreDbName) {
    this.jobstoreDbName = jobstoreDbName;
  }

  public String getJobstoreDbUrl() {
    return jobstoreDbUrl;
  }

  public void setJobstoreDbUrl(String jobstoreDbUrl) {
    this.jobstoreDbUrl = jobstoreDbUrl;
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
}
