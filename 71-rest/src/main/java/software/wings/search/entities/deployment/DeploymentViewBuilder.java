package software.wings.search.entities.deployment;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBObject;
import io.harness.beans.WorkflowType;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowExecution.WorkflowExecutionKeys;
import software.wings.dl.WingsPersistence;
import software.wings.search.framework.EntityInfo;

import java.util.HashSet;
import java.util.Set;

@Singleton
class DeploymentViewBuilder {
  @Inject private WingsPersistence wingsPersistence;
  private DeploymentView deploymentView;

  private void createBaseView(WorkflowExecution workflowExecution) {
    this.deploymentView =
        new DeploymentView(workflowExecution.getUuid(), workflowExecution.getName(), workflowExecution.getCreatedAt(),
            workflowExecution.getCreatedBy(), workflowExecution.getAppId(), workflowExecution.getStatus(),
            workflowExecution.getAppName(), EntityType.DEPLOYMENT, workflowExecution.getAccountId());
  }

  private void setPipeline(WorkflowExecution workflowExecution) {
    if (workflowExecution.getPipelineSummary() != null) {
      deploymentView.setPipelineId(workflowExecution.getPipelineSummary().getPipelineId());
      deploymentView.setPipelineName(workflowExecution.getPipelineSummary().getPipelineName());
    }
  }

  private void setServices(WorkflowExecution workflowExecution) {
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

  private void setEnvironments(WorkflowExecution workflowExecution) {
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

  private void setWorkflowInPipeline(WorkflowExecution workflowExecution) {
    if (workflowExecution.getWorkflowType().equals(WorkflowType.ORCHESTRATION)
        && workflowExecution.getPipelineExecutionId() != null) {
      deploymentView.setWorkflowInPipeline(true);
    }
  }

  private void setWorkflows(WorkflowExecution workflowExecution) {
    if (workflowExecution.getWorkflowType().equals(WorkflowType.PIPELINE)
        && workflowExecution.getWorkflowIds() != null) {
      Set<EntityInfo> workflows = new HashSet<>();
      for (String workflowId : workflowExecution.getWorkflowIds()) {
        Workflow workflow = wingsPersistence.get(Workflow.class, workflowId);
        if (workflow != null) {
          EntityInfo entityInfo = new EntityInfo(workflowId, workflow.getName());
          workflows.add(entityInfo);
        }
      }
      deploymentView.setWorkflows(workflows);
    } else if (workflowExecution.getWorkflowType().equals(WorkflowType.ORCHESTRATION)
        && workflowExecution.getWorkflowId() != null) {
      Workflow workflow = wingsPersistence.get(Workflow.class, workflowExecution.getWorkflowId());
      if (workflow != null) {
        deploymentView.setWorkflowId(workflowExecution.getWorkflowId());
        deploymentView.setWorkflowName(workflow.getName());
      }
    }
  }

  DeploymentView createDeploymentView(WorkflowExecution workflowExecution) {
    createBaseView(workflowExecution);
    setWorkflowInPipeline(workflowExecution);
    setPipeline(workflowExecution);
    setWorkflows(workflowExecution);
    setServices(workflowExecution);
    setEnvironments(workflowExecution);
    return deploymentView;
  }

  DeploymentView createDeploymentView(WorkflowExecution workflowExecution, DBObject changeDocument) {
    createBaseView(workflowExecution);
    setWorkflowInPipeline(workflowExecution);
    if (changeDocument.containsField(WorkflowExecutionKeys.pipelineSummary)) {
      setPipeline(workflowExecution);
    }
    if (changeDocument.containsField(WorkflowExecutionKeys.serviceIds)) {
      setServices(workflowExecution);
    }
    if (changeDocument.containsField(WorkflowExecutionKeys.workflowIds)
        || changeDocument.containsField(WorkflowExecutionKeys.workflowId)) {
      setWorkflows(workflowExecution);
    }
    if (changeDocument.containsField(WorkflowExecutionKeys.envIds)) {
      setEnvironments(workflowExecution);
    }
    return deploymentView;
  }
}
