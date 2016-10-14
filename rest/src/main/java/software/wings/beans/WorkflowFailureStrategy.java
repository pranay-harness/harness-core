package software.wings.beans;

import org.mongodb.morphia.annotations.Entity;

import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Created by rishi on 10/31/16.
 */
@Entity(value = "workflowFailureStrategies", noClassnameStored = true)
public class WorkflowFailureStrategy extends Base {
  @NotNull private String name;
  @NotNull private WorkflowExecutionFilter workflowExecutionFilter;
  @Valid @NotNull @Size(min = 1) private List<FailureStrategy> failureStrategyList = new ArrayList<>();

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public WorkflowExecutionFilter getWorkflowExecutionFilter() {
    return workflowExecutionFilter;
  }

  public void setWorkflowExecutionFilter(WorkflowExecutionFilter workflowExecutionFilter) {
    this.workflowExecutionFilter = workflowExecutionFilter;
  }

  public List<FailureStrategy> getFailureStrategyList() {
    return failureStrategyList;
  }

  public void setFailureStrategyList(List<FailureStrategy> failureStrategyList) {
    this.failureStrategyList = failureStrategyList;
  }

  public static final class WorkflowFailureStrategyBuilder {
    private String name;
    private WorkflowExecutionFilter workflowExecutionFilter;
    private List<FailureStrategy> failureStrategyList = new ArrayList<>();
    private String uuid;
    private String appId;
    private EmbeddedUser createdBy;
    private long createdAt;
    private EmbeddedUser lastUpdatedBy;
    private long lastUpdatedAt;

    private WorkflowFailureStrategyBuilder() {}

    public static WorkflowFailureStrategyBuilder aWorkflowFailureStrategy() {
      return new WorkflowFailureStrategyBuilder();
    }

    public WorkflowFailureStrategyBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public WorkflowFailureStrategyBuilder withWorkflowExecutionFilter(WorkflowExecutionFilter workflowExecutionFilter) {
      this.workflowExecutionFilter = workflowExecutionFilter;
      return this;
    }

    public WorkflowFailureStrategyBuilder addFailureStrategy(FailureStrategy failureStrategy) {
      this.failureStrategyList.add(failureStrategy);
      return this;
    }

    public WorkflowFailureStrategyBuilder withUuid(String uuid) {
      this.uuid = uuid;
      return this;
    }

    public WorkflowFailureStrategyBuilder withAppId(String appId) {
      this.appId = appId;
      return this;
    }

    public WorkflowFailureStrategyBuilder withCreatedBy(EmbeddedUser createdBy) {
      this.createdBy = createdBy;
      return this;
    }

    public WorkflowFailureStrategyBuilder withCreatedAt(long createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public WorkflowFailureStrategyBuilder withLastUpdatedBy(EmbeddedUser lastUpdatedBy) {
      this.lastUpdatedBy = lastUpdatedBy;
      return this;
    }

    public WorkflowFailureStrategyBuilder withLastUpdatedAt(long lastUpdatedAt) {
      this.lastUpdatedAt = lastUpdatedAt;
      return this;
    }

    public WorkflowFailureStrategy build() {
      WorkflowFailureStrategy workflowFailureStrategy = new WorkflowFailureStrategy();
      workflowFailureStrategy.setName(name);
      workflowFailureStrategy.setWorkflowExecutionFilter(workflowExecutionFilter);
      workflowFailureStrategy.setFailureStrategyList(failureStrategyList);
      workflowFailureStrategy.setUuid(uuid);
      workflowFailureStrategy.setAppId(appId);
      workflowFailureStrategy.setCreatedBy(createdBy);
      workflowFailureStrategy.setCreatedAt(createdAt);
      workflowFailureStrategy.setLastUpdatedBy(lastUpdatedBy);
      workflowFailureStrategy.setLastUpdatedAt(lastUpdatedAt);
      return workflowFailureStrategy;
    }
  }
}
