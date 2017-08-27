package software.wings.service.impl.expression;

import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.asList;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.SERVICE_TEMPLATE;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.ServiceVariable.Builder.aServiceVariable;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.dl.PageResponse.Builder.aPageResponse;
import static software.wings.sm.StateType.HTTP;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_VARIABLE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.expression.ExpressionBuilderService;

import java.util.List;
import java.util.Set;
import javax.inject.Inject;

/**
 * Created by sgurubelli on 8/8/17.
 */
public class ExpressionBuilderServiceTest extends WingsBaseTest {
  @Mock private AppService appService;
  @Mock private WorkflowService workflowService;
  @Mock private ServiceVariableService serviceVariableService;

  @Mock private ServiceTemplateService serviceTemplateService;

  @Inject @InjectMocks private ExpressionBuilderService builderService;

  @Inject @InjectMocks private ServiceExpressionBuilder serviceExpressionBuilder;

  @Inject @InjectMocks private EnvironmentExpressionBuilder environmentExpressionBuilder;

  @Inject @InjectMocks private WorkflowExpressionBuilder workflowExpressionBuilder;

  @Test
  public void shouldGetServiceExpressions() {
    when(appService.get(APP_ID)).thenReturn(anApplication().withName(APP_NAME).build());
    Set<String> expressions = builderService.listExpressions(APP_ID, SERVICE_ID, SERVICE);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("service.name"));
  }

  @Test
  public void shouldGetServiceVariableExpressions() {
    when(appService.get(APP_ID)).thenReturn(anApplication().withName(APP_NAME).build());

    PageResponse<ServiceVariable> serviceVariables = aPageResponse()
                                                         .withResponse(asList(aServiceVariable()
                                                                                  .withName(SERVICE_VARIABLE_NAME)
                                                                                  .withEntityId(SERVICE_ID)
                                                                                  .withEntityType(SERVICE)
                                                                                  .build()))
                                                         .build();
    PageRequest<ServiceVariable> pageRequest = aPageRequest()
                                                   .withLimit(UNLIMITED)
                                                   .addFilter("appId", EQ, APP_ID)
                                                   .addFilter("entityId", IN, asList(SERVICE_ID).toArray())
                                                   .addFilter("entityType", EQ, SERVICE)
                                                   .build();

    when(serviceVariableService.list(pageRequest, true)).thenReturn(serviceVariables);
    Set<String> expressions = builderService.listExpressions(APP_ID, SERVICE_ID, SERVICE);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("service.name")).isTrue();
    assertThat(expressions.contains("serviceVariable.SERVICE_VARIABLE_NAME")).isTrue();
  }

  @Test
  public void shouldGetServiceTemplateVariableExpressions() {
    PageResponse<ServiceVariable> serviceVariables = aPageResponse()
                                                         .withResponse(asList(aServiceVariable()
                                                                                  .withName(SERVICE_VARIABLE_NAME)
                                                                                  .withEntityId(SERVICE_ID)
                                                                                  .withEntityType(SERVICE)
                                                                                  .build()))
                                                         .build();
    PageRequest<ServiceVariable> pageRequest = aPageRequest()
                                                   .withLimit(UNLIMITED)
                                                   .addFilter("appId", EQ, APP_ID)
                                                   .addFilter("entityId", IN, asList(SERVICE_ID).toArray())
                                                   .addFilter("entityType", EQ, SERVICE)
                                                   .build();

    when(appService.get(APP_ID)).thenReturn(anApplication().withName(APP_NAME).build());

    when(serviceVariableService.list(pageRequest, true)).thenReturn(serviceVariables);

    PageRequest<ServiceTemplate> serviceTemplatePageRequest =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter("appId", EQ, APP_ID)
            .addFilter("serviceId", IN, asList(SERVICE_ID).toArray())
            .build();
    PageResponse<ServiceTemplate> serviceTemplates =
        aPageResponse()
            .withResponse(
                asList(aServiceTemplate().withUuid(TEMPLATE_ID).withAppId(APP_ID).withServiceId(SERVICE_ID).build()))
            .build();
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, false)).thenReturn(serviceTemplates);

    PageRequest<ServiceVariable> serviceVariablePageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, APP_ID)
            .addFilter("entityId", IN, asList(TEMPLATE_ID).toArray())
            .build();

    serviceVariables =
        aPageResponse()
            .withResponse(asList(
                aServiceVariable().withName("ENV").withEntityId(TEMPLATE_ID).withEntityType(SERVICE_TEMPLATE).build()))
            .build();
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);

    Set<String> expressions = builderService.listExpressions(APP_ID, SERVICE_ID, SERVICE);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("service.name"));
    assertThat(expressions.contains("serviceVariable.SERVICE_VARIABLE_NAME")).isTrue();
    assertThat(expressions.contains("serviceVariable.ENV")).isTrue();
  }

  @Test
  public void shouldGetEnvironmentExpressions() {
    when(appService.get(APP_ID)).thenReturn(anApplication().withName(APP_NAME).build());
    Set<String> expressions = builderService.listExpressions(APP_ID, ENV_ID, ENVIRONMENT, SERVICE_ID);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("env.name"));
  }

  @Test
  public void shouldGetEnvironmentServiceVariableExpressions() {
    when(appService.get(APP_ID)).thenReturn(anApplication().withName(APP_NAME).build());

    PageResponse<ServiceVariable> serviceVariables = aPageResponse()
                                                         .withResponse(asList(aServiceVariable()
                                                                                  .withName(SERVICE_VARIABLE_NAME)
                                                                                  .withEntityId(SERVICE_ID)
                                                                                  .withEntityType(SERVICE)
                                                                                  .build()))
                                                         .build();
    PageRequest<ServiceVariable> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    pageRequest.addFilter("appId", APP_ID, EQ);
    pageRequest.addFilter("entityId", SERVICE_ID, EQ);
    pageRequest.addFilter("entityType", SERVICE, EQ);

    when(serviceVariableService.list(pageRequest, true)).thenReturn(serviceVariables);

    Set<String> expressions = builderService.listExpressions(APP_ID, ENV_ID, ENVIRONMENT, SERVICE_ID);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("env.name"));
    assertThat(expressions.contains("serviceVariable.SERVICE_VARIABLE_NAME"));
  }

  @Test
  public void shouldGetWorkflowExpressions() {
    when(appService.get(APP_ID)).thenReturn(anApplication().withName(APP_NAME).build());
    Set<String> expressions = builderService.listExpressions(APP_ID, WORKFLOW_ID, WORKFLOW, SERVICE_ID);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("env.name"));
  }

  @Test
  public void shouldGetWorkflowVariablesExpressions() {
    List<Variable> userVariables = newArrayList(aVariable().withName("name1").withValue("value1").build());
    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withUserVariables(userVariables)
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    when(appService.get(APP_ID)).thenReturn(anApplication().withName(APP_NAME).build());
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    Set<String> expressions = builderService.listExpressions(APP_ID, WORKFLOW_ID, WORKFLOW, SERVICE_ID);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("env.name")).isTrue();
    assertThat(expressions.contains("workflow.variables.name1")).isTrue();
  }

  @Test
  public void shouldGetWorkflowStateExpressions() {
    List<Variable> userVariables = newArrayList(aVariable().withName("name1").withValue("value1").build());
    Workflow workflow =
        aWorkflow()
            .withName(WORKFLOW_NAME)
            .withAppId(APP_ID)
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withUserVariables(userVariables)
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    when(appService.get(APP_ID)).thenReturn(anApplication().withName(APP_NAME).build());
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    Set<String> expressions = builderService.listExpressions(APP_ID, WORKFLOW_ID, WORKFLOW, SERVICE_ID, HTTP);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("env.name"));
    assertThat(expressions.contains("workflow.variables.name1")).isTrue();
    assertThat(expressions.contains("httpUrl")).isTrue();
  }
}
