package software.wings.graphql.datafetcher.execution;

import io.harness.exception.WingsException;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;

@Value
@Builder
@ToString
public class QLExecutionFilter implements EntityFilter {
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
  private QLIdFilter pipelineExecution;

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
      case PipelineExecution:
        return executionFilter.getPipelineExecution();
      default:
        throw new WingsException("Unsupported type " + executionFilterType);
    }
  }
}
