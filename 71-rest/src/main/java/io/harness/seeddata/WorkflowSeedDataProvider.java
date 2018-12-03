package io.harness.seeddata;

import static io.harness.seeddata.SeedDataProviderConstants.KUBE_WORKFLOW_NAME;
import static java.util.Arrays.asList;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.TemplateExpression;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.Validator;

@Singleton
public class WorkflowSeedDataProvider {
  @Inject private WorkflowService workflowService;

  public Workflow createKubeWorkflow(String appId, String envId, String serviceId, String infraMappingId) {
    Workflow workflow = aWorkflow()
                            .withName(KUBE_WORKFLOW_NAME)
                            .withAppId(appId)
                            .withEnvId(envId)
                            .withServiceId(serviceId)
                            .withInfraMappingId(infraMappingId)
                            .withWorkflowType(WorkflowType.ORCHESTRATION)
                            .withOrchestrationWorkflow(aBasicOrchestrationWorkflow().build())
                            .build();

    Workflow savedWorkflow = workflowService.createWorkflow(workflow);
    Validator.notNullCheck("Workflow not saved", savedWorkflow);

    return savedWorkflow;
  }

  public Workflow templatizeEnvAndServiceInfra(Workflow workflow) {
    TemplateExpression envTemplateExpression = TemplateExpression.builder()
                                                   .fieldName("envId")
                                                   .expression("${Environment}")
                                                   .metadata(ImmutableMap.of("entityType", "ENVIRONMENT"))
                                                   .build();

    TemplateExpression infraTemplateExpression = TemplateExpression.builder()
                                                     .fieldName("infraMappingId")
                                                     .expression("${ServiceInfra_Kubernetes}")
                                                     .metadata(ImmutableMap.of("entityType", "INFRASTRUCTURE_MAPPING"))
                                                     .build();

    workflow.setTemplateExpressions(asList(envTemplateExpression, infraTemplateExpression));

    return workflowService.updateWorkflow(workflow);
  }
}
