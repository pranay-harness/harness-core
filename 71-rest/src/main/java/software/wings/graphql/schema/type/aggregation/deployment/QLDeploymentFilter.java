package software.wings.graphql.schema.type.aggregation.deployment;

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
public class QLDeploymentFilter implements EntityFilter {
  /**
   *   Application(QLDataType.STRING, DeploymentMetaDataFields.APPID)
   *   Service(QLDataType.STRING, DeploymentMetaDataFields.SERVICEID),
   *   Environment(QLDataType.STRING, DeploymentMetaDataFields.ENVID),
   *   CloudProvider(QLDataType.STRING, DeploymentMetaDataFields.CLOUDPROVIDERID),
   *   Status(QLDataType.STRING, DeploymentMetaDataFields.STATUS),
   *   EndTime(QLDataType.NUMBER, DeploymentMetaDataFields.ENDTIME),
   *   StartTime(QLDataType.NUMBER, DeploymentMetaDataFields.STARTTIME),
   *   Duration(QLDataType.NUMBER, DeploymentMetaDataFields.DURATION),
   *   Triggered_By(QLDataType.STRING, DeploymentMetaDataFields.TRIGGERED_BY),
   *   Trigger(QLDataType.STRING, DeploymentMetaDataFields.TRIGGER_ID),
   *   Workflow(QLDataType.STRING, DeploymentMetaDataFields.WORKFLOWID),
   *   Pipeline(QLDataType.STRING, DeploymentMetaDataFields.PIPELINEID);
   */

  private Set<QLDeploymentFilterType> filterTypes;
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

  public QLIdFilter getApplication() {
    if (application != null) {
      getFilterTypes().add(QLDeploymentFilterType.Application);
    }
    return application;
  }

  public QLIdFilter getService() {
    if (service != null) {
      getFilterTypes().add(QLDeploymentFilterType.Service);
    }
    return service;
  }

  public QLIdFilter getCloudProvider() {
    if (cloudProvider != null) {
      getFilterTypes().add(QLDeploymentFilterType.CloudProvider);
    }
    return cloudProvider;
  }

  public QLStringFilter getStatus() {
    if (status != null) {
      getFilterTypes().add(QLDeploymentFilterType.Status);
    }
    return status;
  }

  public QLTimeFilter getEndTime() {
    if (endTime != null) {
      getFilterTypes().add(QLDeploymentFilterType.EndTime);
    }
    return endTime;
  }

  public QLTimeFilter getStartTime() {
    if (startTime != null) {
      getFilterTypes().add(QLDeploymentFilterType.StartTime);
    }
    return startTime;
  }

  public QLNumberFilter getDuration() {
    if (duration != null) {
      getFilterTypes().add(QLDeploymentFilterType.Duration);
    }
    return duration;
  }

  public QLIdFilter getTriggeredBy() {
    if (triggeredBy != null) {
      getFilterTypes().add(QLDeploymentFilterType.TriggeredBy);
    }
    return triggeredBy;
  }

  public QLIdFilter getTrigger() {
    if (trigger != null) {
      getFilterTypes().add(QLDeploymentFilterType.Trigger);
    }
    return trigger;
  }

  public QLIdFilter getWorkflow() {
    if (workflow != null) {
      getFilterTypes().add(QLDeploymentFilterType.Workflow);
    }
    return workflow;
  }

  public QLIdFilter getPipeline() {
    if (pipeline != null) {
      getFilterTypes().add(QLDeploymentFilterType.Pipeline);
    }
    return pipeline;
  }

  public QLIdFilter getEnvironment() {
    if (environment != null) {
      getFilterTypes().add(QLDeploymentFilterType.Environment);
    }
    return environment;
  }

  public Set<QLDeploymentFilterType> getFilterTypes() {
    if (filterTypes == null) {
      filterTypes = new HashSet<>();
    }
    return filterTypes;
  }

  public static Filter getFilter(QLDeploymentFilterType type, QLDeploymentFilter filter) {
    switch (type) {
      case Environment:
        return filter.getEnvironment();
      case Service:
        return filter.getService();
      case Workflow:
        return filter.getWorkflow();
      case CloudProvider:
        return filter.getCloudProvider();
      case Application:
        return filter.getApplication();
      case Status:
        return filter.getStatus();
      case Duration:
        return filter.getDuration();
      case Pipeline:
        return filter.getPipeline();
      case Trigger:
        return filter.getTrigger();
      case TriggeredBy:
        return filter.getTriggeredBy();
      case EndTime:
        return filter.getEndTime();
      case StartTime:
        return filter.getStartTime();
      default:
        throw new WingsException("Unsupported type " + type);
    }
  }
}
