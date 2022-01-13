/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.deployment;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.WorkflowType;

import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.search.framework.EntityInfo;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.DBObject;
import java.util.HashSet;
import java.util.Set;

/**
 * Builder class to build Materialized View of
 * Deployment to be stored in ELK
 *
 * @author ujjawal
 */

@OwnedBy(PL)
@Singleton
class DeploymentViewBuilder {
  @Inject private WingsPersistence wingsPersistence;

  private DeploymentView createBaseView(WorkflowExecution workflowExecution) {
    return new DeploymentView(workflowExecution.getUuid(), workflowExecution.getName(),
        workflowExecution.getCreatedAt(), workflowExecution.getCreatedBy(), workflowExecution.getAppId(),
        workflowExecution.getStatus(), workflowExecution.getAppName(), EntityType.DEPLOYMENT,
        workflowExecution.getAccountId());
  }

  private void setPipeline(WorkflowExecution workflowExecution, DeploymentView deploymentView) {
    if (workflowExecution.getPipelineSummary() != null) {
      deploymentView.setPipelineId(workflowExecution.getPipelineSummary().getPipelineId());
      deploymentView.setPipelineName(workflowExecution.getPipelineSummary().getPipelineName());
    }
  }

  private void setServices(WorkflowExecution workflowExecution, DeploymentView deploymentView) {
    if (workflowExecution.getServiceIds() != null) {
      Set<EntityInfo> services = new HashSet<>();
      for (String serviceId : workflowExecution.getServiceIds()) {
        Service service = wingsPersistence.get(Service.class, serviceId);
        if (service != null) {
          EntityInfo entityInfo = new EntityInfo(serviceId, service.getName());
          services.add(entityInfo);
        }
      }
      deploymentView.setServices(services);
    }
  }

  private void setEnvironments(WorkflowExecution workflowExecution, DeploymentView deploymentView) {
    if (workflowExecution.getEnvIds() != null) {
      Set<EntityInfo> environments = new HashSet<>();
      for (String environmentId : workflowExecution.getEnvIds()) {
        Environment environment = wingsPersistence.get(Environment.class, environmentId);
        if (environment != null) {
          EntityInfo entityInfo = new EntityInfo(environmentId, environment.getName());
          environments.add(entityInfo);
        }
      }
      deploymentView.setEnvironments(environments);
    }
  }

  private void setWorkflowInPipeline(WorkflowExecution workflowExecution, DeploymentView deploymentView) {
    if (workflowExecution.getWorkflowType() == WorkflowType.ORCHESTRATION
        && workflowExecution.getPipelineExecutionId() != null) {
      deploymentView.setWorkflowInPipeline(true);
    }
  }

  private void setWorkflows(WorkflowExecution workflowExecution, DeploymentView deploymentView) {
    if (workflowExecution.getWorkflowType() == WorkflowType.PIPELINE && workflowExecution.getWorkflowIds() != null) {
      Set<EntityInfo> workflows = new HashSet<>();
      for (String workflowId : workflowExecution.getWorkflowIds()) {
        Workflow workflow = wingsPersistence.get(Workflow.class, workflowId);
        if (workflow != null) {
          EntityInfo entityInfo = new EntityInfo(workflowId, workflow.getName());
          workflows.add(entityInfo);
        }
      }
      deploymentView.setWorkflows(workflows);
    } else if (workflowExecution.getWorkflowType() == WorkflowType.ORCHESTRATION
        && workflowExecution.getWorkflowId() != null) {
      Workflow workflow = wingsPersistence.get(Workflow.class, workflowExecution.getWorkflowId());
      if (workflow != null) {
        deploymentView.setWorkflowId(workflowExecution.getWorkflowId());
        deploymentView.setWorkflowName(workflow.getName());
      }
    }
  }

  DeploymentView createDeploymentView(WorkflowExecution workflowExecution) {
    if (wingsPersistence.get(Application.class, workflowExecution.getAppId()) != null) {
      DeploymentView deploymentView = createBaseView(workflowExecution);
      setWorkflowInPipeline(workflowExecution, deploymentView);
      setPipeline(workflowExecution, deploymentView);
      setWorkflows(workflowExecution, deploymentView);
      setServices(workflowExecution, deploymentView);
      setEnvironments(workflowExecution, deploymentView);
      return deploymentView;
    }
    return null;
  }

  DeploymentView createDeploymentView(WorkflowExecution workflowExecution, DBObject changeDocument) {
    if (wingsPersistence.get(Application.class, workflowExecution.getAppId()) != null) {
      DeploymentView deploymentView = createBaseView(workflowExecution);
      setWorkflowInPipeline(workflowExecution, deploymentView);
      if (changeDocument.containsField(WorkflowExecutionKeys.pipelineSummary)) {
        setPipeline(workflowExecution, deploymentView);
      }
      if (changeDocument.containsField(WorkflowExecutionKeys.serviceIds)) {
        setServices(workflowExecution, deploymentView);
      }
      if (changeDocument.containsField(WorkflowExecutionKeys.workflowIds)
          || changeDocument.containsField(WorkflowExecutionKeys.workflowId)) {
        setWorkflows(workflowExecution, deploymentView);
      }
      if (changeDocument.containsField(WorkflowExecutionKeys.envIds)) {
        setEnvironments(workflowExecution, deploymentView);
      }
      return deploymentView;
    }
    return null;
  }
}
