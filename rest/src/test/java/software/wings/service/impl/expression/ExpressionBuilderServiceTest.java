package software.wings.service.impl.expression;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
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
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.common.Constants.HTTP_URL;
import static software.wings.common.Constants.WINGS_BACKUP_PATH;
import static software.wings.common.Constants.WINGS_RUNTIME_PATH;
import static software.wings.common.Constants.WINGS_STAGING_PATH;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.sm.StateType.AWS_CODEDEPLOY_STATE;
import static software.wings.sm.StateType.COMMAND;
import static software.wings.sm.StateType.HTTP;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_VARIABLE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.inject.Inject;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.ServiceVariable;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.expression.ExpressionBuilderService;

import java.util.List;
import java.util.Set;

/**
 * Created by sgurubelli on 8/8/17.
 */
public class ExpressionBuilderServiceTest extends WingsBaseTest {
  @Mock private AppService appService;
  @Mock private WorkflowService workflowService;
  @Mock private ServiceVariableService serviceVariableService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private ServiceResourceService serviceResourceService;

  @Inject @InjectMocks private ExpressionBuilderService builderService;
  @Inject @InjectMocks private ServiceExpressionBuilder serviceExpressionBuilder;
  @Inject @InjectMocks private EnvironmentExpressionBuilder environmentExpressionBuilder;
  @Inject @InjectMocks private WorkflowExpressionBuilder workflowExpressionBuilder;

  PageRequest<ServiceVariable> serviceVariablePageRequest = aPageRequest()
                                                                .withLimit(UNLIMITED)
                                                                .addFilter("appId", EQ, APP_ID)
                                                                .addFilter("entityId", IN, asList(SERVICE_ID).toArray())
                                                                .addFilter("entityType", EQ, SERVICE)
                                                                .build();

  PageResponse<ServiceVariable> serviceVariables =
      aPageResponse()
          .withResponse(asList(
              ServiceVariable.builder().name(SERVICE_VARIABLE_NAME).entityId(SERVICE_ID).entityType(SERVICE).build()))
          .build();

  PageRequest<ServiceVariable> envServiceVariablePageRequest = aPageRequest()
                                                                   .withLimit(PageRequest.UNLIMITED)
                                                                   .addFilter("appId", EQ, APP_ID)
                                                                   .addFilter("entityId", IN, asList(ENV_ID).toArray())
                                                                   .addFilter("entityType", EQ, ENVIRONMENT)
                                                                   .build();

  PageRequest<ServiceVariable> serviceTemplateServiceVariablePageRequest =
      aPageRequest()
          .withLimit(PageRequest.UNLIMITED)
          .addFilter("appId", EQ, APP_ID)
          .addFilter("entityId", IN, asList(TEMPLATE_ID).toArray())
          .addFilter("entityType", EQ, SERVICE_TEMPLATE)
          .build();

  PageResponse<ServiceVariable> envServiceVariables =
      aPageResponse()
          .withResponse(asList(ServiceVariable.builder().name("ENV").entityId(ENV_ID).entityType(ENVIRONMENT).build()))
          .build();

  PageResponse<ServiceVariable> envServiceOverrideVariables =
      aPageResponse()
          .withResponse(asList(
              ServiceVariable.builder().name("ENVOverride").entityId(TEMPLATE_ID).entityType(SERVICE_TEMPLATE).build()))
          .build();

  PageRequest<ServiceTemplate> serviceTemplatePageRequest =
      aPageRequest()
          .withLimit(UNLIMITED)
          .addFilter("appId", EQ, APP_ID)
          .addFilter("serviceId", IN, asList(SERVICE_ID).toArray())
          .build();
  PageResponse<ServiceTemplate> serviceTemplates = aPageResponse()
                                                       .withResponse(asList(aServiceTemplate()
                                                                                .withUuid(TEMPLATE_ID)
                                                                                .withEnvId(ENV_ID)
                                                                                .withAppId(APP_ID)
                                                                                .withServiceId(SERVICE_ID)
                                                                                .build()))
                                                       .build();

  @Before
  public void setUp() {
    when(appService.get(APP_ID)).thenReturn(anApplication().withName(APP_NAME).build());
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, false)).thenReturn(aPageResponse().build());
    when(serviceVariableService.list(envServiceVariablePageRequest, true)).thenReturn(serviceVariables);
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, false)).thenReturn(aPageResponse().build());
  }

  @Test
  public void shouldGetServiceExpressions() {
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, false)).thenReturn(aPageResponse().build());
    Set<String> expressions = builderService.listExpressions(APP_ID, SERVICE_ID, SERVICE);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("service.name");
  }

  @Test
  public void shouldGetServiceExpressionsCommand() {
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, false)).thenReturn(aPageResponse().build());
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);

    Set<String> expressions = builderService.listExpressions(APP_ID, SERVICE_ID, SERVICE, SERVICE_ID, COMMAND);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("service.name");
    assertThat(expressions).contains("serviceVariable.SERVICE_VARIABLE_NAME");
    assertThat(expressions).contains(WINGS_RUNTIME_PATH);
    assertThat(expressions).contains(WINGS_BACKUP_PATH);
    assertThat(expressions).contains(WINGS_STAGING_PATH);
  }

  @Test
  public void shouldGetServiceVariableExpressions() {
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, false)).thenReturn(aPageResponse().build());
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);

    Set<String> expressions = builderService.listExpressions(APP_ID, SERVICE_ID, SERVICE);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("service.name")).isTrue();
    assertThat(expressions.contains("serviceVariable.SERVICE_VARIABLE_NAME")).isTrue();
  }

  @Test
  public void shouldGetAllServiceVariableExpressions() {
    when(serviceResourceService.list(
             aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, APP_ID).addFieldsIncluded("uuid").build(),
             false, false))
        .thenReturn(aPageResponse()
                        .withResponse(asList(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build()))
                        .build());
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, false)).thenReturn(aPageResponse().build());

    Set<String> expressions = builderService.listExpressions(APP_ID, "All", SERVICE);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("service.name")).isTrue();
    assertThat(expressions.contains("serviceVariable.SERVICE_VARIABLE_NAME")).isTrue();
  }

  @Test
  public void shouldGetServiceTemplateVariableExpressions() {
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, false)).thenReturn(serviceTemplates);
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);
    when(serviceVariableService.list(envServiceVariablePageRequest, true)).thenReturn(envServiceVariables);
    when(serviceVariableService.list(serviceTemplateServiceVariablePageRequest, true))
        .thenReturn(envServiceOverrideVariables);
    Set<String> expressions = builderService.listExpressions(APP_ID, SERVICE_ID, SERVICE);

    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("service.name");
    assertThat(expressions).contains("serviceVariable.SERVICE_VARIABLE_NAME");
    assertThat(expressions).contains("serviceVariable.ENV");
    assertThat(expressions).contains("serviceVariable.ENVOverride");
  }

  @Test
  public void shouldGetEnvironmentExpressions() {
    when(serviceTemplateService.list(any(PageRequest.class), anyBoolean(), anyBoolean()))
        .thenReturn(aPageResponse().build());

    Set<String> expressions = builderService.listExpressions(APP_ID, ENV_ID, ENVIRONMENT, SERVICE_ID);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("env.name");
  }

  @Test
  public void shouldGetEnvironmentServiceVariableExpressions() {
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);
    when(serviceTemplateService.list(any(PageRequest.class), anyBoolean(), anyBoolean())).thenReturn(serviceTemplates);

    Set<String> expressions = builderService.listExpressions(APP_ID, ENV_ID, ENVIRONMENT, SERVICE_ID);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("env.name");
    assertThat(expressions).contains("serviceVariable.SERVICE_VARIABLE_NAME");
  }

  @Test
  public void shouldGetEnvironmentServiceVariableOverridesExpressions() {
    PageRequest<ServiceVariable> serviceVariablePageRequest = aPageRequest()
                                                                  .withLimit(PageRequest.UNLIMITED)
                                                                  .addFilter("appId", EQ, APP_ID)
                                                                  .addFilter("entityId", IN, asList(ENV_ID).toArray())
                                                                  .addFilter("entityType", EQ, ENVIRONMENT)
                                                                  .build();
    serviceVariables = aPageResponse()
                           .withResponse(asList(
                               ServiceVariable.builder().name("ENV").entityId(ENV_ID).entityType(ENVIRONMENT).build()))
                           .build();
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);
    when(serviceTemplateService.list(any(PageRequest.class), anyBoolean(), anyBoolean())).thenReturn(serviceTemplates);

    when(serviceVariableService.list(serviceTemplateServiceVariablePageRequest, true))
        .thenReturn(envServiceOverrideVariables);

    Set<String> expressions = builderService.listExpressions(APP_ID, ENV_ID, ENVIRONMENT);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("env.name");
    assertThat(expressions).contains("serviceVariable.ENV");
    assertThat(expressions).contains("serviceVariable.ENVOverride");
  }

  @Test
  public void shouldGetWorkflowExpressions() {
    PageRequest<ServiceVariable> serviceVariablePageRequest = aPageRequest()
                                                                  .withLimit(PageRequest.UNLIMITED)
                                                                  .addFilter("appId", EQ, APP_ID)
                                                                  .addFilter("entityId", IN, asList(ENV_ID).toArray())
                                                                  .addFilter("entityType", EQ, ENVIRONMENT)
                                                                  .build();
    serviceVariables = aPageResponse()
                           .withResponse(asList(
                               ServiceVariable.builder().name("ENV").entityId(ENV_ID).entityType(ENVIRONMENT).build()))
                           .build();
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);

    Set<String> expressions = builderService.listExpressions(APP_ID, WORKFLOW_ID, WORKFLOW, SERVICE_ID);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("env.name");
  }

  @Test
  public void shouldGetWorkflowVariablesExpressions() {
    List<Variable> userVariables = newArrayList(aVariable().withName("name1").withValue("value1").build());
    Workflow workflow = buildCanaryWorkflow(userVariables);

    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);

    PageRequest<ServiceVariable> serviceVariablePageRequest = aPageRequest()
                                                                  .withLimit(PageRequest.UNLIMITED)
                                                                  .addFilter("appId", EQ, APP_ID)
                                                                  .addFilter("entityId", IN, asList(ENV_ID).toArray())
                                                                  .build();
    serviceVariables = aPageResponse()
                           .withResponse(asList(
                               ServiceVariable.builder().name("ENV").entityId(ENV_ID).entityType(ENVIRONMENT).build()))
                           .build();
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);

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
            .withEnvId(ENV_ID)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withUserVariables(userVariables)
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                    .build())
            .build();

    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    PageRequest<ServiceVariable> serviceVariablePageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, APP_ID)
            .addFilter("entityId", IN, asList(TEMPLATE_ID).toArray())
            .build();
    serviceVariables =
        aPageResponse()
            .withResponse(asList(
                ServiceVariable.builder().name("ENV").entityId(TEMPLATE_ID).entityType(SERVICE_TEMPLATE).build()))
            .build();
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);

    Set<String> expressions = builderService.listExpressions(APP_ID, WORKFLOW_ID, WORKFLOW, SERVICE_ID, HTTP);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("env.name");
    assertThat(expressions).contains("workflow.variables.name1");
    assertThat(expressions).contains(HTTP_URL);
  }

  @Test
  public void shouldGetWorkflowStateExpressionsAllService() {
    when(serviceResourceService.list(
             aPageRequest().withLimit(UNLIMITED).addFilter("appId", EQ, APP_ID).addFieldsIncluded("uuid").build(),
             false, false))
        .thenReturn(aPageResponse()
                        .withResponse(asList(Service.builder().uuid(SERVICE_ID).name(SERVICE_NAME).build()))
                        .build());
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, false)).thenReturn(aPageResponse().build());

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

    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    PageRequest<ServiceVariable> serviceVariablePageRequest = aPageRequest()
                                                                  .withLimit(PageRequest.UNLIMITED)
                                                                  .addFilter("appId", EQ, APP_ID)
                                                                  .addFilter("entityId", IN, asList(ENV_ID).toArray())
                                                                  .addFilter("entityType", EQ, ENVIRONMENT)
                                                                  .build();
    Set<String> expressions = builderService.listExpressions(APP_ID, WORKFLOW_ID, WORKFLOW, "All", HTTP);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("env.name");
    assertThat(expressions).contains("workflow.variables.name1");
    assertThat(expressions).contains(HTTP_URL);
  }

  @Test
  public void shouldGetWorkflowCodeDeployStateExpressions() {
    List<Variable> userVariables = newArrayList(aVariable().withName("name1").withValue("value1").build());
    Workflow workflow = buildCanaryWorkflow(userVariables);

    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);

    PageRequest<ServiceVariable> serviceVariablePageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter("appId", EQ, APP_ID)
            .addFilter("entityId", IN, asList(TEMPLATE_ID).toArray())
            .build();
    serviceVariables =
        aPageResponse()
            .withResponse(asList(
                ServiceVariable.builder().name("ENV").entityId(TEMPLATE_ID).entityType(SERVICE_TEMPLATE).build()))
            .build();
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);

    Set<String> expressions =
        builderService.listExpressions(APP_ID, WORKFLOW_ID, WORKFLOW, SERVICE_ID, AWS_CODEDEPLOY_STATE);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("env.name");
    assertThat(expressions).contains("workflow.variables.name1");
    assertThat(expressions).contains("artifact.bucketName");
  }

  @Test
  public void shouldGetWorkflowCommandStateExpressions() {
    List<Variable> userVariables = newArrayList(aVariable().withName("name1").withValue("value1").build());
    Workflow workflow = buildCanaryWorkflow(userVariables);

    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    when(serviceVariableService.list(serviceVariablePageRequest, true)).thenReturn(serviceVariables);

    Set<String> expressions = builderService.listExpressions(APP_ID, WORKFLOW_ID, WORKFLOW, SERVICE_ID, COMMAND);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("env.name");
    assertThat(expressions).contains("workflow.variables.name1");
    assertThat(expressions).contains(WINGS_STAGING_PATH);
  }

  private Workflow buildCanaryWorkflow(List<Variable> userVariables) {
    return aWorkflow()
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
  }
}
