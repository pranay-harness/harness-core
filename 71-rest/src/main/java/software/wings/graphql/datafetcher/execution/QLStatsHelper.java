package software.wings.graphql.datafetcher.execution;

import static software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields.APPID;
import static software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields.CLOUDPROVIDERID;
import static software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields.ENVID;
import static software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields.PIPELINEID;
import static software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields.SERVICEID;
import static software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields.STATUS;
import static software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields.TRIGGEREDBY;
import static software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields.TRIGGERID;
import static software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields.WORKFLOWID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.trigger.Trigger;
import software.wings.dl.WingsPersistence;
import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields;

@Singleton
@Slf4j
public class QLStatsHelper {
  @Inject WingsPersistence wingsPersistence;

  String getEntityName(DeploymentMetaDataFields field, String entityId) {
    switch (field) {
      case APPID:
        return getApplicationName(entityId);
      case CLOUDPROVIDERID:
        return getSettingsAttributeName(entityId);
      case STATUS:
        return entityId;
      case TRIGGERID:
        return getTriggerName(entityId);
      case TRIGGEREDBY:
        return getUserName(entityId);
      case PIPELINEID:
        return getPipelineName(entityId);
      case SERVICEID:
        return getServiceName(entityId);
      case ENVID:
        return getEnvName(entityId);
      case WORKFLOWID:
        return getWorkflowName(entityId);
      default:
        throw new RuntimeException("Invalid EntityType " + field);
    }
  }

  private String getWorkflowName(String entityId) {
    Workflow workflow = wingsPersistence.get(Workflow.class, entityId);
    if (workflow != null) {
      return workflow.getName();
    }
    return entityId;
  }

  private String getEnvName(String entityId) {
    Environment environment = wingsPersistence.get(Environment.class, entityId);
    if (environment != null) {
      return environment.getName();
    }
    return entityId;
  }

  private String getServiceName(String entityId) {
    Service service = wingsPersistence.get(Service.class, entityId);
    if (service != null) {
      return service.getName();
    }
    return entityId;
  }

  private String getApplicationName(String entityId) {
    Application application = wingsPersistence.get(Application.class, entityId);
    if (application != null) {
      return application.getName();
    } else {
      return entityId;
    }
  }

  private String getSettingsAttributeName(String entityId) {
    SettingAttribute attribute = wingsPersistence.get(SettingAttribute.class, entityId);
    if (attribute != null) {
      return attribute.getName();
    } else {
      return entityId;
    }
  }

  private String getPipelineName(String entityId) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, entityId);
    if (pipeline != null) {
      return pipeline.getName();
    } else {
      return entityId;
    }
  }

  private String getUserName(String userId) {
    User user = wingsPersistence.get(User.class, userId);
    if (user == null) {
      return userId;
    }
    return user.getName();
  }

  private String getTriggerName(String triggerId) {
    Trigger trigger = wingsPersistence.get(Trigger.class, triggerId);
    if (trigger != null) {
      return trigger.getName();
    }
    return triggerId;
  }
}
