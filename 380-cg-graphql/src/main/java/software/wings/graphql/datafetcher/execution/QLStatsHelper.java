/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.execution;

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
import software.wings.graphql.utils.nameservice.NameResult;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

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
      case TRIGGER_ID:
        return getTriggerName(entityId);
      case TRIGGERED_BY:
        return getUserName(entityId);
      case PIPELINEID:
        return getPipelineName(entityId);
      case SERVICEID:
        return getServiceName(entityId);
      case ENVID:
        return getEnvName(entityId);
      case ENVTYPE:
        return entityId;
      case WORKFLOWID:
        return getWorkflowName(entityId);
      case TAGS:
        return entityId;
      default:
        throw new RuntimeException("Invalid EntityType " + field);
    }
  }

  private String getWorkflowName(String entityId) {
    Workflow workflow = wingsPersistence.get(Workflow.class, entityId);
    if (workflow != null) {
      return workflow.getName();
    }
    return NameResult.DELETED;
  }

  private String getEnvName(String entityId) {
    Environment environment = wingsPersistence.get(Environment.class, entityId);
    if (environment != null) {
      return environment.getName();
    }
    return NameResult.DELETED;
  }

  private String getServiceName(String entityId) {
    Service service = wingsPersistence.get(Service.class, entityId);
    if (service != null) {
      return service.getName();
    }
    return NameResult.DELETED;
  }

  private String getApplicationName(String entityId) {
    Application application = wingsPersistence.get(Application.class, entityId);
    if (application != null) {
      return application.getName();
    } else {
      return NameResult.DELETED;
    }
  }

  private String getSettingsAttributeName(String entityId) {
    SettingAttribute attribute = wingsPersistence.get(SettingAttribute.class, entityId);
    if (attribute != null) {
      return attribute.getName();
    } else {
      return NameResult.DELETED;
    }
  }

  private String getPipelineName(String entityId) {
    Pipeline pipeline = wingsPersistence.get(Pipeline.class, entityId);
    if (pipeline != null) {
      return pipeline.getName();
    } else {
      return NameResult.DELETED;
    }
  }

  private String getUserName(String userId) {
    User user = wingsPersistence.get(User.class, userId);
    if (user == null) {
      return NameResult.DELETED;
    }
    return user.getName();
  }

  private String getTriggerName(String triggerId) {
    Trigger trigger = wingsPersistence.get(Trigger.class, triggerId);
    if (trigger != null) {
      return trigger.getName();
    }
    return NameResult.DELETED;
  }
}
