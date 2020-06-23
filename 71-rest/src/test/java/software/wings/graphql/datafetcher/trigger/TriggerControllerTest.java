package software.wings.graphql.datafetcher.trigger;

import static io.harness.rule.OwnerRule.MILAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.Trigger;
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@PrepareForTest(TriggerController.class)
@RunWith(PowerMockRunner.class)
public class TriggerControllerTest extends CategoryTest {
  @Mock TriggerConditionController triggerConditionController;
  @Mock TriggerActionController triggerActionController;

  @InjectMocks TriggerController triggerController = PowerMockito.spy(new TriggerController());

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
  public void prepareTriggerShouldReturnTrigger() throws Exception {
    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder()
            .triggerId("triggerId")
            .name("name")
            .applicationId("appId")
            .description("desc")
            .action(QLTriggerActionInput.builder().entityId("entityId").executionType(QLExecutionType.WORKFLOW).build())
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

    Mockito.when(triggerActionController.resolveArtifactSelections(Matchers.any(QLCreateOrUpdateTriggerInput.class)))
        .thenReturn(artifactSelections);

    Mockito.when(triggerActionController.resolveWorkflowVariables(Matchers.any(QLCreateOrUpdateTriggerInput.class)))
        .thenReturn(workflowVariables);

    Mockito.when(triggerConditionController.resolveTriggerCondition(Matchers.any(QLCreateOrUpdateTriggerInput.class)))
        .thenReturn(artifactTriggerCondition);

    PowerMockito.doNothing().when(triggerController, "validateTrigger", qlCreateOrUpdateTriggerInput, "accountId");

    Trigger trigger = triggerController.prepareTrigger(qlCreateOrUpdateTriggerInput, "accountId");

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
