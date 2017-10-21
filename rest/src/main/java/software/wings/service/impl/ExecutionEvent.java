package software.wings.service.impl;

import org.mongodb.morphia.annotations.Entity;
import software.wings.core.queue.Queuable;

@Entity(value = "executionQueue", noClassnameStored = true)
public class ExecutionEvent extends Queuable {
  private String appId;
  private String workflowId;

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public static final class ExecutionEventBuilder {
    private String appId;
    private String workflowId;

    private ExecutionEventBuilder() {}

    public static ExecutionEventBuilder anExecutionEvent() {
      return new ExecutionEventBuilder();
    }

    public ExecutionEventBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public ExecutionEventBuilder withWorkflowId(String workflowId) {
      this.workflowId = workflowId;
      return this;
    }

    public ExecutionEvent build() {
      ExecutionEvent executionEvent = new ExecutionEvent();
      executionEvent.setAppId(appId);
      executionEvent.setWorkflowId(workflowId);
      return executionEvent;
    }
  }
}
