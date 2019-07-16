package software.wings.graphql.datafetcher.execution;

import io.harness.exception.WingsException;
import lombok.Builder;
import lombok.ToString;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

import java.util.HashSet;
import java.util.Set;

@Builder
@ToString
public class QLExecutionFilter implements EntityFilter {
  private Set<QLExecutionFilterType> filterTypes;
  private QLIdFilter application;
  private QLIdFilter service;
  private QLIdFilter cloudProvider;
  private QLIdFilter environment;
  private QLStringFilter status;
  private QLTimeFilter endTime;
  private QLTimeFilter startTime;
  private QLNumberFilter duration;
  private QLIdFilter triggeredBy;
  private QLIdFilter trigger;
  private QLIdFilter workflow;
  private QLIdFilter pipeline;
  private QLTimeFilter creationTime;

  public QLIdFilter getApplication() {
    if (application != null) {
      getFilterTypes().add(QLExecutionFilterType.Application);
    }
    return application;
  }

  public QLIdFilter getService() {
    if (service != null) {
      getFilterTypes().add(QLExecutionFilterType.Service);
    }
    return service;
  }

  public QLIdFilter getCloudProvider() {
    if (cloudProvider != null) {
      getFilterTypes().add(QLExecutionFilterType.CloudProvider);
    }
    return cloudProvider;
  }

  public QLStringFilter getStatus() {
    if (status != null) {
      getFilterTypes().add(QLExecutionFilterType.Status);
    }
    return status;
  }

  public QLTimeFilter getEndTime() {
    if (endTime != null) {
      getFilterTypes().add(QLExecutionFilterType.EndTime);
    }
    return endTime;
  }

  public QLTimeFilter getStartTime() {
    if (startTime != null) {
      getFilterTypes().add(QLExecutionFilterType.StartTime);
    }
    return startTime;
  }

  public QLNumberFilter getDuration() {
    if (duration != null) {
      getFilterTypes().add(QLExecutionFilterType.Duration);
    }
    return duration;
  }

  public QLIdFilter getTriggeredBy() {
    if (triggeredBy != null) {
      getFilterTypes().add(QLExecutionFilterType.TriggeredBy);
    }
    return triggeredBy;
  }

  public QLIdFilter getTrigger() {
    if (trigger != null) {
      getFilterTypes().add(QLExecutionFilterType.Trigger);
    }
    return trigger;
  }

  public QLIdFilter getWorkflow() {
    if (workflow != null) {
      getFilterTypes().add(QLExecutionFilterType.Workflow);
    }
    return workflow;
  }

  public QLIdFilter getPipeline() {
    if (pipeline != null) {
      getFilterTypes().add(QLExecutionFilterType.Pipeline);
    }
    return pipeline;
  }

  public QLIdFilter getEnvironment() {
    if (environment != null) {
      getFilterTypes().add(QLExecutionFilterType.Environment);
    }
    return environment;
  }

  public Set<QLExecutionFilterType> getFilterTypes() {
    if (filterTypes == null) {
      filterTypes = new HashSet<>();
    }
    return filterTypes;
  }

  public QLTimeFilter getCreationTime() {
    if (environment != null) {
      getFilterTypes().add(QLExecutionFilterType.CreatedAt);
    }
    return creationTime;
  }

  public static Filter getFilter(QLExecutionFilterType executionFilterType, QLExecutionFilter executionFilter) {
    switch (executionFilterType) {
      case Environment:
        return executionFilter.getEnvironment();
      case Service:
        return executionFilter.getService();
      case Workflow:
        return executionFilter.getWorkflow();
      case CloudProvider:
        return executionFilter.getCloudProvider();
      case Application:
        return executionFilter.getApplication();
      case Status:
        return executionFilter.getStatus();
      case Duration:
        return executionFilter.getDuration();
      case Pipeline:
        return executionFilter.getPipeline();
      case Trigger:
        return executionFilter.getTrigger();
      case TriggeredBy:
        return executionFilter.getTriggeredBy();
      case EndTime:
        return executionFilter.getEndTime();
      case StartTime:
        return executionFilter.getStartTime();
      case CreatedAt:
        return executionFilter.getCreationTime();
      default:
        throw new WingsException("Unsupported type " + executionFilterType);
    }
  }
}
