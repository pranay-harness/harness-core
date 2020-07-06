package software.wings.graphql.datafetcher.trigger;

import static io.harness.rule.OwnerRule.MILAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.datafetcher.execution.PipelineExecutionController;
import software.wings.graphql.datafetcher.execution.WorkflowExecutionController;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.type.trigger.QLArtifactSelection;
import software.wings.graphql.schema.type.trigger.QLCreateOrUpdateTriggerInput;
import software.wings.graphql.schema.type.trigger.QLLastCollected;
import software.wings.graphql.schema.type.trigger.QLOnPipelineCompletion;
import software.wings.graphql.schema.type.trigger.QLTrigger;
import software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerBuilder;
import software.wings.graphql.schema.type.trigger.QLTriggerActionInput;
import software.wings.graphql.schema.type.trigger.QLTriggerConditionType;
import software.wings.graphql.schema.type.trigger.QLTriggerPayload;
import software.wings.graphql.schema.type.trigger.QLTriggerVariableValue;
import software.wings.graphql.schema.type.trigger.QLWorkflowAction;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.impl.security.auth.DeploymentAuthHandler;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerControllerTest extends CategoryTest {
  @Mock TriggerConditionController triggerConditionController;
  @Mock TriggerActionController triggerActionController;
  @Mock WorkflowService workflowService;
  @Mock PipelineService pipelineService;
  @Mock WorkflowExecutionController workflowExecutionController;
  @Mock PipelineExecutionController pipelineExecutionController;
  @Mock DeploymentAuthHandler deploymentAuthHandler;
  @Mock AuthHandler authHandler;

  @InjectMocks TriggerController triggerController = Mockito.spy(new TriggerController());

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerShouldReturnTriggerObject() {
    PipelineTriggerCondition pipelineTriggerCondition =
        PipelineTriggerCondition.builder().pipelineId("pipelineId").pipelineName("pipelineName").build();

    EmbeddedUser embeddedUser = EmbeddedUser.builder().name("name").email("email").uuid("uuid").build();

    Trigger trigger = Trigger.builder()
                          .workflowName("workflowName")
                          .workflowId("workflowId")
                          .condition(pipelineTriggerCondition)
                          .description("description")
                          .uuid("uuid")
                          .name("trigger")
                          .workflowType(WorkflowType.ORCHESTRATION)
                          .createdAt(13123213123L)
                          .createdBy(embeddedUser)
                          .build();

    QLOnPipelineCompletion qlOnPipelineCompletionMock =
        QLOnPipelineCompletion.builder()
            .pipelineId("pipelineId")
            .pipelineName("pipelineName")
            .triggerConditionType(QLTriggerConditionType.PIPELINE_COMPLETION)
            .build();

    Mockito.when(triggerConditionController.populateTriggerCondition(Matchers.any(Trigger.class), Matchers.anyString()))
        .thenReturn(qlOnPipelineCompletionMock);

    List<QLArtifactSelection> artifactSelections = Arrays.asList(QLLastCollected.builder()
                                                                     .artifactSourceName("sourceName")
                                                                     .artifactSourceId("sourceId")
                                                                     .regex(true)
                                                                     .artifactFilter("filter")
                                                                     .serviceId("serviceId")
                                                                     .serviceName("serviceName")
                                                                     .build());

    List<QLTriggerVariableValue> variableValues =
        Arrays.asList(QLTriggerVariableValue.builder().name("varName").value("varValue").build());

    QLWorkflowAction qlWorkflowActionMock = QLWorkflowAction.builder()
                                                .workflowId("workflowId")
                                                .workflowName("workflowName")
                                                .artifactSelections(artifactSelections)
                                                .variables(variableValues)
                                                .build();

    Mockito.when(triggerActionController.populateTriggerAction(Matchers.any(Trigger.class)))
        .thenReturn(qlWorkflowActionMock);

    QLTriggerBuilder qlTriggerBuilder = QLTrigger.builder();
    triggerController.populateTrigger(trigger, qlTriggerBuilder, "accountId");
    QLTrigger qlTrigger = qlTriggerBuilder.build();

    assertThat(qlTrigger).isNotNull();
    assertThat(qlTrigger.getId()).isEqualTo(trigger.getUuid());
    assertThat(qlTrigger.getName()).isEqualTo(trigger.getName());
    assertThat(qlTrigger.getDescription()).isEqualTo(trigger.getDescription());
    assertThat(qlTrigger.getCreatedAt()).isEqualTo(trigger.getCreatedAt());
    assertThat(qlTrigger.getExcludeHostsWithSameArtifact()).isEqualTo(false);

    assertThat(qlTrigger.getCondition()).isNotNull();
    QLOnPipelineCompletion qlOnPipelineCompletion = (QLOnPipelineCompletion) qlTrigger.getCondition();
    assertThat(qlOnPipelineCompletion.getTriggerConditionType())
        .isEqualByComparingTo(QLTriggerConditionType.PIPELINE_COMPLETION);
    assertThat(qlOnPipelineCompletion.getPipelineName()).isEqualTo(pipelineTriggerCondition.getPipelineName());
    assertThat(qlOnPipelineCompletion.getPipelineId()).isEqualTo(pipelineTriggerCondition.getPipelineId());

    assertThat(qlTrigger.getAction()).isNotNull();
    QLWorkflowAction qlWorkflowAction = (QLWorkflowAction) qlTrigger.getAction();

    assertThat(qlWorkflowAction.getWorkflowId()).isEqualTo(trigger.getWorkflowId());
    assertThat(qlWorkflowAction.getWorkflowName()).isEqualTo(trigger.getWorkflowName());

    assertThat(qlWorkflowAction.getArtifactSelections()).isNotNull();
    assertThat(qlWorkflowAction.getArtifactSelections()).isNotEmpty();
    assertThat(qlWorkflowAction.getArtifactSelections().get(0)).isNotNull();
    QLLastCollected qlLastCollected = (QLLastCollected) qlWorkflowAction.getArtifactSelections().get(0);
    QLLastCollected qlLastCollectedMock = (QLLastCollected) artifactSelections.get(0);

    assertThat(qlLastCollected.getArtifactFilter()).isEqualTo(qlLastCollectedMock.getArtifactFilter());
    assertThat(qlLastCollected.getArtifactSourceId()).isEqualTo(qlLastCollectedMock.getArtifactSourceId());
    assertThat(qlLastCollected.getArtifactSourceName()).isEqualTo(qlLastCollectedMock.getArtifactSourceName());
    assertThat(qlLastCollected.getServiceId()).isEqualTo(qlLastCollectedMock.getServiceId());
    assertThat(qlLastCollected.getServiceName()).isEqualTo(qlLastCollectedMock.getServiceName());
    assertThat(qlLastCollected.getRegex()).isEqualTo(qlLastCollectedMock.getRegex());

    assertThat(qlWorkflowAction.getVariables()).isNotNull();
    assertThat(qlWorkflowAction.getVariables()).isNotEmpty();
    assertThat(qlWorkflowAction.getVariables().get(0)).isNotNull();
    assertThat(qlWorkflowAction.getVariables().get(0).getName()).isEqualTo("varName");
    assertThat(qlWorkflowAction.getVariables().get(0).getValue()).isEqualTo("varValue");

    assertThat(qlTrigger.getCreatedBy()).isNotNull();
    assertThat(qlTrigger.getCreatedBy().getId()).isEqualTo(trigger.getCreatedBy().getUuid());
    assertThat(qlTrigger.getCreatedBy().getName()).isEqualTo(trigger.getCreatedBy().getName());
    assertThat(qlTrigger.getCreatedBy().getEmail()).isEqualTo(trigger.getCreatedBy().getEmail());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void prepareQLTriggerShouldReturnQLTriggerPayload() {
    QLOnPipelineCompletion qlOnPipelineCompletionMock =
        QLOnPipelineCompletion.builder()
            .pipelineId("pipelineId")
            .pipelineName("pipelineName")
            .triggerConditionType(QLTriggerConditionType.PIPELINE_COMPLETION)
            .build();

    Mockito.when(triggerConditionController.populateTriggerCondition(Matchers.any(Trigger.class), Matchers.anyString()))
        .thenReturn(qlOnPipelineCompletionMock);

    List<QLArtifactSelection> artifactSelections = Arrays.asList(QLLastCollected.builder()
                                                                     .artifactSourceName("sourceName")
                                                                     .artifactSourceId("sourceId")
                                                                     .regex(true)
                                                                     .artifactFilter("filter")
                                                                     .serviceId("serviceId")
                                                                     .serviceName("serviceName")
                                                                     .build());

    List<QLTriggerVariableValue> variableValues =
        Arrays.asList(QLTriggerVariableValue.builder().name("varName").value("varValue").build());

    QLWorkflowAction qlWorkflowActionMock = QLWorkflowAction.builder()
                                                .workflowId("workflowId")
                                                .workflowName("workflowName")
                                                .artifactSelections(artifactSelections)
                                                .variables(variableValues)
                                                .build();

    Mockito.when(triggerActionController.populateTriggerAction(Matchers.any(Trigger.class)))
        .thenReturn(qlWorkflowActionMock);

    EmbeddedUser embeddedUser = EmbeddedUser.builder().name("name").email("email").uuid("uuid").build();

    Trigger trigger = Trigger.builder()
                          .workflowName("workflowName")
                          .workflowId("workflowId")
                          .description("description")
                          .uuid("uuid")
                          .name("trigger")
                          .workflowType(WorkflowType.ORCHESTRATION)
                          .createdAt(13123213123L)
                          .createdBy(embeddedUser)
                          .build();

    QLTriggerPayload qlTriggerPayload = triggerController.prepareQLTrigger(trigger, "mutationId", "accountId");
    assertThat(qlTriggerPayload).isNotNull();
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void prepareTriggerInOrchestrationWorkflowShouldReturnTrigger() throws Exception {
    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput = QLCreateOrUpdateTriggerInput.builder()
                                                                    .triggerId("triggerId")
                                                                    .name("name")
                                                                    .applicationId("appId")
                                                                    .description("desc")
                                                                    .action(QLTriggerActionInput.builder()
                                                                                .entityId("entityId")
                                                                                .executionType(QLExecutionType.WORKFLOW)
                                                                                .artifactSelections(new ArrayList<>())
                                                                                .variables(new ArrayList<>())
                                                                                .build())
                                                                    .build();

    List<ArtifactSelection> artifactSelections = Arrays.asList(ArtifactSelection.builder()
                                                                   .type(ArtifactSelection.Type.LAST_DEPLOYED)
                                                                   .serviceId("serviceId")
                                                                   .serviceName("serviceName")
                                                                   .artifactStreamId("artifactStreamId")
                                                                   .artifactSourceName("sourceName")
                                                                   .artifactFilter("artifactFilter")
                                                                   .workflowId("workflowId")
                                                                   .workflowName("workflowName")
                                                                   .regex(true)
                                                                   .build());

    Map<String, String> workflowVariables = new HashMap<>();
    workflowVariables.put("ID", "value");

    ArtifactTriggerCondition artifactTriggerCondition = ArtifactTriggerCondition.builder()
                                                            .artifactStreamId("artifactStreamId")
                                                            .artifactSourceName("artifactSourceName")
                                                            .artifactFilter("artifactFilter")
                                                            .regex(true)
                                                            .build();

    Mockito.doNothing()
        .when(triggerController)
        .validateTrigger(Matchers.any(QLCreateOrUpdateTriggerInput.class), Matchers.anyString());
    Mockito
        .when(workflowService.readWorkflow(
            qlCreateOrUpdateTriggerInput.getApplicationId(), qlCreateOrUpdateTriggerInput.getAction().getEntityId()))
        .thenReturn(Mockito.mock(Workflow.class));
    Mockito.doNothing()
        .when(triggerController)
        .validateWorkflow(Matchers.anyString(), Matchers.anyString(), Matchers.any(Workflow.class));
    Mockito.when(workflowExecutionController.resolveEnvId(Matchers.any(Workflow.class), Matchers.anyList()))
        .thenReturn("envId");
    Mockito.doNothing()
        .when(deploymentAuthHandler)
        .authorizeWorkflowExecution(Matchers.anyString(), Matchers.anyString());
    Mockito.doNothing().when(authHandler).checkIfUserAllowedToDeployToEnv(Matchers.anyString(), Matchers.anyString());
    Mockito
        .when(triggerActionController.validateAndResolveWorkflowVariables(
            Matchers.anyList(), Matchers.any(Workflow.class), Matchers.anyString()))
        .thenReturn(workflowVariables);
    Mockito.doNothing()
        .when(triggerController)
        .validateWorkflowArtifactSourceServiceIds(Matchers.anyMap(), Matchers.anyList(), Matchers.any(Workflow.class));
    Mockito.when(triggerActionController.resolveArtifactSelections(Matchers.any(QLCreateOrUpdateTriggerInput.class)))
        .thenReturn(artifactSelections);
    Mockito.when(triggerConditionController.resolveTriggerCondition(Matchers.any(QLCreateOrUpdateTriggerInput.class)))
        .thenReturn(artifactTriggerCondition);

    Trigger trigger = triggerController.prepareTrigger(qlCreateOrUpdateTriggerInput, "accountId");

    Mockito.verify(triggerController, Mockito.times(1))
        .validateTrigger(Matchers.any(QLCreateOrUpdateTriggerInput.class), Matchers.anyString());
    Mockito.verify(workflowService, Mockito.times(1)).readWorkflow(Matchers.anyString(), Matchers.anyString());
    Mockito.verify(triggerController, Mockito.times(1))
        .validateWorkflow(Matchers.anyString(), Matchers.anyString(), Matchers.any(Workflow.class));
    Mockito.verify(workflowExecutionController, Mockito.times(1))
        .resolveEnvId(Matchers.any(Workflow.class), Matchers.anyList());
    Mockito.verify(deploymentAuthHandler, Mockito.times(1))
        .authorizeWorkflowExecution(Matchers.anyString(), Matchers.anyString());
    Mockito.verify(authHandler, Mockito.times(1))
        .checkIfUserAllowedToDeployToEnv(Matchers.anyString(), Matchers.anyString());
    Mockito.verify(triggerActionController, Mockito.times(1))
        .validateAndResolveWorkflowVariables(Matchers.anyList(), Matchers.any(Workflow.class), Matchers.anyString());
    Mockito.verify(triggerController, Mockito.times(1))
        .validateWorkflowArtifactSourceServiceIds(Matchers.anyMap(), Matchers.anyList(), Matchers.any(Workflow.class));
    Mockito.verify(triggerActionController, Mockito.times(1))
        .resolveArtifactSelections(Matchers.any(QLCreateOrUpdateTriggerInput.class));
    Mockito.verify(triggerConditionController, Mockito.times(1))
        .resolveTriggerCondition(Matchers.any(QLCreateOrUpdateTriggerInput.class));

    assertThat(trigger).isNotNull();
    assertThat(trigger.getUuid()).isEqualTo(qlCreateOrUpdateTriggerInput.getTriggerId());
    assertThat(trigger.getName()).isEqualTo(qlCreateOrUpdateTriggerInput.getName());
    assertThat(trigger.getDescription()).isEqualTo(qlCreateOrUpdateTriggerInput.getDescription());
    assertThat(trigger.getWorkflowId()).isEqualTo(qlCreateOrUpdateTriggerInput.getAction().getEntityId());

    assertThat(trigger.getArtifactSelections()).isNotNull();
    assertThat(trigger.getArtifactSelections()).isNotEmpty();
    assertThat(trigger.getArtifactSelections().get(0).getArtifactStreamId())
        .isEqualTo(artifactSelections.get(0).getArtifactStreamId());
    assertThat(trigger.getArtifactSelections().get(0).getArtifactSourceName())
        .isEqualTo(artifactSelections.get(0).getArtifactSourceName());
    assertThat(trigger.getArtifactSelections().get(0).getArtifactFilter())
        .isEqualTo(artifactSelections.get(0).getArtifactFilter());
    assertThat(trigger.getArtifactSelections().get(0).isRegex()).isEqualTo(artifactSelections.get(0).isRegex());

    assertThat(trigger.getWorkflowVariables()).isNotNull();
    assertThat(trigger.getWorkflowVariables().get("ID")).isEqualTo("value");

    ArtifactTriggerCondition returnedArtifactTriggerCondition = (ArtifactTriggerCondition) trigger.getCondition();
    assertThat(returnedArtifactTriggerCondition).isNotNull();
    assertThat(returnedArtifactTriggerCondition.getArtifactStreamId())
        .isEqualTo(artifactTriggerCondition.getArtifactStreamId());
    assertThat(returnedArtifactTriggerCondition.getArtifactSourceName())
        .isEqualTo(artifactTriggerCondition.getArtifactSourceName());
    assertThat(returnedArtifactTriggerCondition.getArtifactFilter())
        .isEqualTo(artifactTriggerCondition.getArtifactFilter());
    assertThat(returnedArtifactTriggerCondition.isRegex()).isEqualTo(artifactTriggerCondition.isRegex());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void prepareTriggerInPipelineWorkflowShouldReturnTrigger() throws Exception {
    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput = QLCreateOrUpdateTriggerInput.builder()
                                                                    .triggerId("triggerId")
                                                                    .name("name")
                                                                    .applicationId("appId")
                                                                    .description("desc")
                                                                    .action(QLTriggerActionInput.builder()
                                                                                .entityId("entityId")
                                                                                .executionType(QLExecutionType.PIPELINE)
                                                                                .artifactSelections(new ArrayList<>())
                                                                                .variables(new ArrayList<>())
                                                                                .build())
                                                                    .build();

    List<ArtifactSelection> artifactSelections = Arrays.asList(ArtifactSelection.builder()
                                                                   .type(ArtifactSelection.Type.LAST_DEPLOYED)
                                                                   .serviceId("serviceId")
                                                                   .serviceName("serviceName")
                                                                   .artifactStreamId("artifactStreamId")
                                                                   .artifactSourceName("sourceName")
                                                                   .artifactFilter("artifactFilter")
                                                                   .pipelineId("pipelineId")
                                                                   .pipelineName("pipelineName")
                                                                   .regex(true)
                                                                   .build());

    Map<String, String> workflowVariables = new HashMap<>();
    workflowVariables.put("ID", "value");

    ArtifactTriggerCondition artifactTriggerCondition = ArtifactTriggerCondition.builder()
                                                            .artifactStreamId("artifactStreamId")
                                                            .artifactSourceName("artifactSourceName")
                                                            .artifactFilter("artifactFilter")
                                                            .regex(true)
                                                            .build();

    Mockito.doNothing()
        .when(triggerController)
        .validateTrigger(Matchers.any(QLCreateOrUpdateTriggerInput.class), Matchers.anyString());
    Mockito.when(pipelineService.readPipeline(Matchers.anyString(), Matchers.anyString(), Matchers.anyBoolean()))
        .thenReturn(Mockito.mock(Pipeline.class));
    Mockito.doNothing()
        .when(triggerController)
        .validatePipeline(Matchers.anyString(), Matchers.anyString(), Matchers.any(Pipeline.class));
    Mockito.when(pipelineExecutionController.resolveEnvId(Matchers.any(Pipeline.class), Matchers.anyList()))
        .thenReturn("envId");
    Mockito.doNothing()
        .when(deploymentAuthHandler)
        .authorizePipelineExecution(Matchers.anyString(), Matchers.anyString());
    Mockito
        .when(triggerActionController.validateAndResolvePipelineVariables(
            Matchers.anyList(), Matchers.any(Pipeline.class), Matchers.anyString()))
        .thenReturn(workflowVariables);
    Mockito.doNothing()
        .when(triggerController)
        .validatePipelineArtifactSourceServiceIds(Matchers.anyMap(), Matchers.anyList(), Matchers.any(Pipeline.class));
    Mockito.when(triggerActionController.resolveArtifactSelections(Matchers.any(QLCreateOrUpdateTriggerInput.class)))
        .thenReturn(artifactSelections);
    Mockito.when(triggerConditionController.resolveTriggerCondition(Matchers.any(QLCreateOrUpdateTriggerInput.class)))
        .thenReturn(artifactTriggerCondition);

    Mockito.doNothing()
        .when(triggerController)
        .validateTrigger(Matchers.any(QLCreateOrUpdateTriggerInput.class), Matchers.anyString());

    Trigger trigger = triggerController.prepareTrigger(qlCreateOrUpdateTriggerInput, "accountId");

    Mockito.verify(triggerController, Mockito.times(1))
        .validateTrigger(Matchers.any(QLCreateOrUpdateTriggerInput.class), Matchers.anyString());
    Mockito.verify(pipelineService, Mockito.times(1))
        .readPipeline(Matchers.anyString(), Matchers.anyString(), Matchers.anyBoolean());
    Mockito.verify(triggerController, Mockito.times(1))
        .validatePipeline(Matchers.anyString(), Matchers.anyString(), Matchers.any(Pipeline.class));
    Mockito.verify(pipelineExecutionController, Mockito.times(1))
        .resolveEnvId(Matchers.any(Pipeline.class), Matchers.anyList());
    Mockito.verify(deploymentAuthHandler, Mockito.times(1))
        .authorizePipelineExecution(Matchers.anyString(), Matchers.anyString());
    Mockito.verify(triggerActionController, Mockito.times(1))
        .validateAndResolvePipelineVariables(Matchers.anyList(), Matchers.any(Pipeline.class), Matchers.anyString());
    Mockito.verify(triggerController, Mockito.times(1))
        .validatePipelineArtifactSourceServiceIds(Matchers.anyMap(), Matchers.anyList(), Matchers.any(Pipeline.class));
    Mockito.verify(triggerActionController, Mockito.times(1))
        .resolveArtifactSelections(Matchers.any(QLCreateOrUpdateTriggerInput.class));
    Mockito.verify(triggerConditionController, Mockito.times(1))
        .resolveTriggerCondition(Matchers.any(QLCreateOrUpdateTriggerInput.class));

    assertThat(trigger).isNotNull();
    assertThat(trigger.getUuid()).isEqualTo(qlCreateOrUpdateTriggerInput.getTriggerId());
    assertThat(trigger.getName()).isEqualTo(qlCreateOrUpdateTriggerInput.getName());
    assertThat(trigger.getDescription()).isEqualTo(qlCreateOrUpdateTriggerInput.getDescription());
    assertThat(trigger.getWorkflowId()).isEqualTo(qlCreateOrUpdateTriggerInput.getAction().getEntityId());

    assertThat(trigger.getArtifactSelections()).isNotNull();
    assertThat(trigger.getArtifactSelections()).isNotEmpty();
    assertThat(trigger.getArtifactSelections().get(0).getArtifactStreamId())
        .isEqualTo(artifactSelections.get(0).getArtifactStreamId());
    assertThat(trigger.getArtifactSelections().get(0).getArtifactSourceName())
        .isEqualTo(artifactSelections.get(0).getArtifactSourceName());
    assertThat(trigger.getArtifactSelections().get(0).getArtifactFilter())
        .isEqualTo(artifactSelections.get(0).getArtifactFilter());
    assertThat(trigger.getArtifactSelections().get(0).isRegex()).isEqualTo(artifactSelections.get(0).isRegex());

    assertThat(trigger.getWorkflowVariables()).isNotNull();
    assertThat(trigger.getWorkflowVariables().get("ID")).isEqualTo("value");

    ArtifactTriggerCondition returnedArtifactTriggerCondition = (ArtifactTriggerCondition) trigger.getCondition();
    assertThat(returnedArtifactTriggerCondition).isNotNull();
    assertThat(returnedArtifactTriggerCondition.getArtifactStreamId())
        .isEqualTo(artifactTriggerCondition.getArtifactStreamId());
    assertThat(returnedArtifactTriggerCondition.getArtifactSourceName())
        .isEqualTo(artifactTriggerCondition.getArtifactSourceName());
    assertThat(returnedArtifactTriggerCondition.getArtifactFilter())
        .isEqualTo(artifactTriggerCondition.getArtifactFilter());
    assertThat(returnedArtifactTriggerCondition.isRegex()).isEqualTo(artifactTriggerCondition.isRegex());
  }
}
