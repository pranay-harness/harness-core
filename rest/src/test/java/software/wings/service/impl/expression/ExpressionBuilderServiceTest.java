package software.wings.service.impl.expression;

import static com.google.common.truth.Truth.assertThat;
import static org.assertj.core.util.Lists.newArrayList;
import static org.mockito.Mockito.when;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.StateType.HTTP;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.expression.ExpressionBuilderService;

import java.util.List;
import javax.inject.Inject;

/**
 * Created by sgurubelli on 8/8/17.
 */
public class ExpressionBuilderServiceTest extends WingsBaseTest {
  @Mock private AppService appService;
  @Mock private WorkflowService workflowService;

  @Inject @InjectMocks private ExpressionBuilderService builderService;

  @Test
  public void shouldGetServiceExpressions() {
    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withName(APP_NAME).build());
    List<String> expressions = builderService.listExpressions(APP_ID, SERVICE_ID, SERVICE);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("service.name"));
  }

  @Test
  public void shouldGetEnvironmentExpressions() {
    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withName(APP_NAME).build());
    List<String> expressions = builderService.listExpressions(APP_ID, ENV_ID, ENVIRONMENT, SERVICE_ID);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("env.name"));
  }

  @Test
  public void shouldGetWorkflowExpressions() {
    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withName(APP_NAME).build());
    List<String> expressions = builderService.listExpressions(APP_ID, WORKFLOW_ID, WORKFLOW, SERVICE_ID);
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

    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withName(APP_NAME).build());
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    List<String> expressions = builderService.listExpressions(APP_ID, WORKFLOW_ID, WORKFLOW, SERVICE_ID);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("env.name"));
    assertThat(expressions.contains("workflow.variables.name1"));
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

    when(appService.get(APP_ID)).thenReturn(Application.Builder.anApplication().withName(APP_NAME).build());
    when(workflowService.readWorkflow(APP_ID, WORKFLOW_ID)).thenReturn(workflow);
    List<String> expressions = builderService.listExpressions(APP_ID, WORKFLOW_ID, WORKFLOW, SERVICE_ID, HTTP);
    assertThat(expressions).isNotNull();
    assertThat(expressions.contains("env.name"));
    assertThat(expressions.contains("workflow.variables.name1"));
    assertThat(expressions.contains("httpUrl"));
  }
}
