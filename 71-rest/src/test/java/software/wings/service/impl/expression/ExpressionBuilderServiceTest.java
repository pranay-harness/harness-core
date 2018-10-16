package software.wings.service.impl.expression;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.EntityType.APPLICATION;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.SERVICE_TEMPLATE;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.common.Constants.HTTP_URL;
import static software.wings.common.Constants.WINGS_BACKUP_PATH;
import static software.wings.common.Constants.WINGS_RUNTIME_PATH;
import static software.wings.common.Constants.WINGS_STAGING_PATH;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.MASKED;
import static software.wings.service.intfc.ServiceVariableService.EncryptedFieldMode.OBTAIN_VALUE;
import static software.wings.sm.StateType.AWS_CODEDEPLOY_STATE;
import static software.wings.sm.StateType.COMMAND;
import static software.wings.sm.StateType.HTTP;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_VARIABLE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
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
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.expression.ExpressionBuilderService;

import java.util.List;
import java.util.Set;

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
    when(appService.getApplicationWithDefaults(APP_ID))
        .thenReturn(anApplication()
                        .withName(APP_NAME)
                        .withAccountId(ACCOUNT_ID)
                        .withDescription("Awesome app")
                        .withDefaults(ImmutableMap.of("Param1", "Value1"))
                        .build());
    when(serviceVariableService.list(serviceVariablePageRequest, MASKED)).thenReturn(serviceVariables);
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, OBTAIN_VALUE))
        .thenReturn(aPageResponse().build());
    when(serviceVariableService.list(envServiceVariablePageRequest, MASKED)).thenReturn(serviceVariables);
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, OBTAIN_VALUE))
        .thenReturn(aPageResponse().build());
  }

  @Test
  public void shouldGetServiceExpressions() {
    when(serviceVariableService.list(serviceVariablePageRequest, MASKED)).thenReturn(serviceVariables);
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, OBTAIN_VALUE))
        .thenReturn(aPageResponse().build());
    Set<String> expressions = builderService.listExpressions(APP_ID, SERVICE_ID, SERVICE);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("service.name");
  }

  @Test
  public void shouldGetApplDefaultsExpressions() {
    Set<String> expressions = builderService.listExpressions(APP_ID, APP_ID, APPLICATION);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("app.name");
    assertThat(expressions).contains("app.defaults.Param1");
    assertThat(expressions).contains("app.description");
    assertThat(expressions).contains("service.name");
    assertThat(expressions).contains("service.description");
    assertThat(expressions).contains("workflow.name");
    assertThat(expressions).contains("workflow.description");
    assertThat(expressions).contains("pipeline.name", "pipeline.description");
  }

  @Test
  public void shouldGetServiceExpressionsCommand() {
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, OBTAIN_VALUE))
        .thenReturn(aPageResponse().build());
    when(serviceVariableService.list(serviceVariablePageRequest, MASKED)).thenReturn(serviceVariables);

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
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, OBTAIN_VALUE))
        .thenReturn(aPageResponse().build());
    when(serviceVariableService.list(serviceVariablePageRequest, MASKED)).thenReturn(serviceVariables);

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
    when(serviceVariableService.list(serviceVariablePageRequest, MASKED)).thenReturn(serviceVariables);
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, OBTAIN_VALUE))
        .thenReturn(aPageResponse().build());

    Set<String> expressions = builderService.listExpressions(APP_ID, "All", SERVICE);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("service.name")).isTrue();
    assertThat(expressions.contains("serviceVariable.SERVICE_VARIABLE_NAME")).isTrue();
  }

  @Test
  public void shouldGetServiceTemplateVariableExpressions() {
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, OBTAIN_VALUE)).thenReturn(serviceTemplates);
    when(serviceVariableService.list(serviceVariablePageRequest, MASKED)).thenReturn(serviceVariables);
    when(serviceVariableService.list(envServiceVariablePageRequest, MASKED)).thenReturn(envServiceVariables);
    when(serviceVariableService.list(serviceTemplateServiceVariablePageRequest, MASKED))
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
    when(serviceTemplateService.list(any(PageRequest.class), anyBoolean(), any())).thenReturn(aPageResponse().build());

    Set<String> expressions = builderService.listExpressions(APP_ID, ENV_ID, ENVIRONMENT, SERVICE_ID);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("env.name");
  }

  @Test
  public void shouldGetEnvironmentServiceVariableExpressions() {
    when(serviceVariableService.list(serviceVariablePageRequest, MASKED)).thenReturn(serviceVariables);
    when(serviceTemplateService.list(any(PageRequest.class), anyBoolean(), any())).thenReturn(serviceTemplates);

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
    when(serviceVariableService.list(serviceVariablePageRequest, MASKED)).thenReturn(serviceVariables);
    when(serviceTemplateService.list(any(PageRequest.class), anyBoolean(), any())).thenReturn(serviceTemplates);

    when(serviceVariableService.list(serviceTemplateServiceVariablePageRequest, MASKED))
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
    when(serviceVariableService.list(serviceVariablePageRequest, MASKED)).thenReturn(serviceVariables);

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
    when(serviceVariableService.list(serviceVariablePageRequest, MASKED)).thenReturn(serviceVariables);

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
    when(serviceVariableService.list(serviceVariablePageRequest, MASKED)).thenReturn(serviceVariables);

    Set<String> expressions = builderService.listExpressions(APP_ID, WORKFLOW_ID, WORKFLOW, SERVICE_ID, HTTP);
    assertThat(expressions).isNotNull();
    assertThat(expressions).contains("env.name");
    assertThat(expressions).contains("workflow.variables.name1");
    assertThat(expressions).contains("workflow.name", "workflow.startTs", "pipeline.name", "pipeline.startTs");
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
    when(serviceTemplateService.list(serviceTemplatePageRequest, false, OBTAIN_VALUE))
        .thenReturn(aPageResponse().build());

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
    when(serviceVariableService.list(serviceVariablePageRequest, MASKED)).thenReturn(serviceVariables);

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
    when(serviceVariableService.list(serviceVariablePageRequest, MASKED)).thenReturn(serviceVariables);

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
