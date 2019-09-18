package software.wings.search.entities.workflow;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.DBObject;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.OrchestrationWorkflow;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.dl.WingsPersistence;
import software.wings.search.framework.EntityInfo;
import software.wings.service.intfc.ServiceResourceService;

import java.util.HashSet;
import java.util.Set;

@Singleton
public class WorkflowViewBuilder {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  private static final String TEMPLATIZED = "Templatized - ";
  private WorkflowView workflowView;

  private void populateServicesInWorkflow(Workflow workflow) {
    OrchestrationWorkflow orchestrationWorkflow = workflow.getOrchestration();
    if (orchestrationWorkflow != null) {
      workflow.setServices(
          serviceResourceService.fetchServicesByUuids(workflow.getAppId(), orchestrationWorkflow.getServiceIds()));
    }
  }

  private void createBaseView(Workflow workflow) {
    this.workflowView = new WorkflowView(workflow.getUuid(), workflow.getName(), workflow.getDescription(),
        workflow.getAccountId(), workflow.getCreatedAt(), workflow.getLastUpdatedAt(), EntityType.WORKFLOW,
        workflow.getCreatedBy(), workflow.getLastUpdatedBy(), workflow.getAppId(),
        workflow.getOrchestration().getOrchestrationWorkflowType().name());
  }

  private void setApplicationName(Workflow workflow) {
    if (workflow.getAppId() != null) {
      Application application = wingsPersistence.get(Application.class, workflow.getAppId());
      workflowView.setAppName(application.getName());
    }
  }

  private void setEnvironment(Workflow workflow) {
    boolean isEnvironmentTemplatized = false;

    if (workflow.getTemplateExpressions() != null) {
      for (TemplateExpression templateExpression : workflow.getTemplateExpressions()) {
        if (templateExpression.getFieldName().equals("envId")) {
          workflowView.setEnvironmentName(TEMPLATIZED + templateExpression.getExpression());
          isEnvironmentTemplatized = true;
        }
      }
    }

    if (!isEnvironmentTemplatized && workflow.getEnvId() != null) {
      Environment environment = wingsPersistence.get(Environment.class, workflow.getEnvId());
      workflowView.setEnvironmentId(environment.getUuid());
      workflowView.setEnvironmentName(environment.getName());
    }
  }

  private void setServices(Workflow workflow) {
    populateServicesInWorkflow(workflow);
    if (workflow.getServices() != null) {
      Set<EntityInfo> serviceInfos = new HashSet<>();
      for (Service service : workflow.getServices()) {
        EntityInfo serviceInfo = new EntityInfo(service.getUuid(), service.getName());
        serviceInfos.add(serviceInfo);
      }
      workflowView.setServices(serviceInfos);
    }
  }

  public WorkflowView createWorkflowView(Workflow workflow) {
    createBaseView(workflow);
    setApplicationName(workflow);
    setEnvironment(workflow);
    setServices(workflow);
    return workflowView;
  }

  public WorkflowView createWorkflowView(Workflow workflow, DBObject changeDocument) {
    createBaseView(workflow);
    if (changeDocument.containsField(WorkflowKeys.appId)) {
      setApplicationName(workflow);
    }
    if (changeDocument.containsField(WorkflowKeys.templateExpressions)) {
      setEnvironment(workflow);
    }
    if (changeDocument.containsField(WorkflowKeys.orchestration)) {
      setServices(workflow);
    }
    return workflowView;
  }
}
