package software.wings.graphql.schema.type.aggregation.deployment;

import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import software.wings.graphql.schema.type.aggregation.EntityFilter;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentTypeFilter;

import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@ToString
public class QLDeploymentFilter implements EntityFilter {
  /**
   *   Application(DeploymentMetaDataFields.APPID),
   *   Service(DeploymentMetaDataFields.SERVICEID),
   *   Environment(DeploymentMetaDataFields.ENVID),
   *   EnvironmentType(DeploymentMetaDataFields.ENVTYPES),
   *   CloudProvider(DeploymentMetaDataFields.CLOUDPROVIDERID),
   *   Status(DeploymentMetaDataFields.STATUS),
   *   EndTime(DeploymentMetaDataFields.ENDTIME),
   *   StartTime(DeploymentMetaDataFields.STARTTIME),
   *   Duration(DeploymentMetaDataFields.DURATION),
   *   TriggeredBy(DeploymentMetaDataFields.TRIGGERED_BY),
   *   Trigger(DeploymentMetaDataFields.TRIGGER_ID),
   *   Workflow(DeploymentMetaDataFields.WORKFLOWID),
   *   Pipeline(DeploymentMetaDataFields.PIPELINEID),
   *   RollbackDuration(DeploymentMetaDataFields.ROLLBACK_DURATION),;
   */

  private QLIdFilter application;
  private QLIdFilter service;
  private QLIdFilter cloudProvider;
  private QLIdFilter environment;
  private QLEnvironmentTypeFilter environmentType;
  private QLStringFilter status;
  private QLTimeFilter endTime;
  private QLTimeFilter startTime;
  private QLNumberFilter duration;
  private QLNumberFilter rollbackDuration;
  private QLIdFilter triggeredBy;
  private QLIdFilter trigger;
  private QLIdFilter workflow;
  private QLIdFilter pipeline;
  private QLDeploymentTagFilter tag;
  private QLDeploymentTagFilter tags;

  public static Set<QLDeploymentFilterType> getFilterTypes(QLDeploymentFilter filter) {
    Set<QLDeploymentFilterType> filterTypes = new HashSet<>();
    if (filter.getEnvironment() != null) {
      filterTypes.add(QLDeploymentFilterType.Environment);
    }
    if (filter.getEnvironmentType() != null) {
      filterTypes.add(QLDeploymentFilterType.EnvironmentType);
    }

    if (filter.getPipeline() != null) {
      filterTypes.add(QLDeploymentFilterType.Pipeline);
    }
    if (filter.getWorkflow() != null) {
      filterTypes.add(QLDeploymentFilterType.Workflow);
    }
    if (filter.getTrigger() != null) {
      filterTypes.add(QLDeploymentFilterType.Trigger);
    }
    if (filter.getTriggeredBy() != null) {
      filterTypes.add(QLDeploymentFilterType.TriggeredBy);
    }
    if (filter.getDuration() != null) {
      filterTypes.add(QLDeploymentFilterType.Duration);
    }
    if (filter.getStartTime() != null) {
      filterTypes.add(QLDeploymentFilterType.StartTime);
    }
    if (filter.getEndTime() != null) {
      filterTypes.add(QLDeploymentFilterType.EndTime);
    }
    if (filter.getStatus() != null) {
      filterTypes.add(QLDeploymentFilterType.Status);
    }
    if (filter.getCloudProvider() != null) {
      filterTypes.add(QLDeploymentFilterType.CloudProvider);
    }

    if (filter.getService() != null) {
      filterTypes.add(QLDeploymentFilterType.Service);
    }

    if (filter.getApplication() != null) {
      filterTypes.add(QLDeploymentFilterType.Application);
    }

    if (filter.getRollbackDuration() != null) {
      filterTypes.add(QLDeploymentFilterType.RollbackDuration);
    }

    if (filter.getTag() != null) {
      filterTypes.add(QLDeploymentFilterType.Tag);
    }

    if (filter.getTags() != null) {
      filterTypes.add(QLDeploymentFilterType.Tags);
    }

    return filterTypes;
  }

  public static Filter getFilter(QLDeploymentFilterType type, QLDeploymentFilter filter) {
    switch (type) {
      case Environment:
        return filter.getEnvironment();
      case EnvironmentType:
        return filter.getEnvironmentType();
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
      case RollbackDuration:
        return filter.getRollbackDuration();
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
      case Tag:
        return filter.getTag();
      case Tags:
        return filter.getTags();
      default:
        throw new InvalidRequestException("Unsupported type " + type);
    }
  }
}
