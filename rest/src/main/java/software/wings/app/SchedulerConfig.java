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
}
