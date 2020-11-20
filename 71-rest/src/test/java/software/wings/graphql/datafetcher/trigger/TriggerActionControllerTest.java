package software.wings.graphql.datafetcher.trigger;

import static io.harness.rule.OwnerRule.MILAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
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
import software.wings.beans.Service;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactSelection.Type;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.schema.mutation.execution.input.QLExecutionType;
import software.wings.graphql.schema.type.trigger.QLArtifactSelectionInput;
import software.wings.graphql.schema.type.trigger.QLArtifactSelectionType;
import software.wings.graphql.schema.type.trigger.QLCreateOrUpdateTriggerInput;
import software.wings.graphql.schema.type.trigger.QLFromTriggeringPipeline;
import software.wings.graphql.schema.type.trigger.QLFromWebhookPayload;
import software.wings.graphql.schema.type.trigger.QLLastCollected;
import software.wings.graphql.schema.type.trigger.QLLastDeployedFromPipeline;
import software.wings.graphql.schema.type.trigger.QLLastDeployedFromWorkflow;
import software.wings.graphql.schema.type.trigger.QLPipelineAction;
import software.wings.graphql.schema.type.trigger.QLTriggerActionInput;
import software.wings.graphql.schema.type.trigger.QLTriggerConditionInput;
import software.wings.graphql.schema.type.trigger.QLWorkflowAction;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerActionControllerTest extends CategoryTest {
  @Mock PipelineService pipelineService;
  @Mock ServiceResourceService serviceResourceService;

  @InjectMocks TriggerActionController triggerActionController = Mockito.spy(new TriggerActionController());

  @Before
  public void initMocks() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerActionShouldReturnPipelineWorkflowWithFromTriggeringArtifactSourceService() {
    ArtifactSelection artifactSource = ArtifactSelection.builder()
                                           .serviceId("serviceId")
                                           .serviceName("serviceName")
                                           .type(Type.ARTIFACT_SOURCE)
                                           .build();
    List<ArtifactSelection> artifactSelections = Arrays.asList(artifactSource);
    Map<String, String> variables = new HashMap<>();
    variables.put("variableKey", "variableResult");
    Trigger trigger = Trigger.builder()
                          .workflowId("workflowId")
                          .workflowName("workflowName")
                          .artifactSelections(artifactSelections)
                          .workflowVariables(variables)
                          .build();

    QLPipelineAction qlPipelineAction = (QLPipelineAction) triggerActionController.populateTriggerAction(trigger);

    assertThat(qlPipelineAction).isNotNull();
    assertThat(qlPipelineAction.getPipelineId()).isEqualTo(trigger.getWorkflowId());
    assertThat(qlPipelineAction.getPipelineName()).isEqualTo(trigger.getWorkflowName());

    assertThat(qlPipelineAction.getArtifactSelections()).isNotNull();
    assertThat(qlPipelineAction.getArtifactSelections().get(0).getServiceId())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceId());
    assertThat(qlPipelineAction.getArtifactSelections().get(0).getServiceName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceName());

    assertThat(qlPipelineAction.getVariables()).isNotNull();
    assertThat(qlPipelineAction.getVariables().get(0).getValue())
        .isEqualTo(trigger.getWorkflowVariables().get("variableKey"));
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerActionShouldReturnPipelineWorkflowWitQLLastCollectedService() {
    ArtifactSelection artifactSource = ArtifactSelection.builder()
                                           .serviceId("serviceId")
                                           .serviceName("serviceName")
                                           .artifactStreamId("streamId")
                                           .artifactSourceName("sourceName")
                                           .artifactFilter("filter")
                                           .regex(true)
                                           .type(Type.LAST_COLLECTED)
                                           .build();
    List<ArtifactSelection> artifactSelections = Arrays.asList(artifactSource);

    Map<String, String> variables = new HashMap<>();
    variables.put("variableKey", "variableResult");
    Trigger trigger = Trigger.builder()
                          .workflowId("workflowId")
                          .workflowName("workflowName")
                          .artifactSelections(artifactSelections)
                          .workflowVariables(variables)
                          .build();

    QLPipelineAction qlPipelineAction = (QLPipelineAction) triggerActionController.populateTriggerAction(trigger);

    assertThat(qlPipelineAction).isNotNull();
    assertThat(qlPipelineAction.getPipelineId()).isEqualTo(trigger.getWorkflowId());
    assertThat(qlPipelineAction.getPipelineName()).isEqualTo(trigger.getWorkflowName());

    assertThat(qlPipelineAction.getArtifactSelections()).isNotNull();
    QLLastCollected qlLastCollected = (QLLastCollected) qlPipelineAction.getArtifactSelections().get(0);
    assertThat(qlLastCollected.getServiceId()).isEqualTo(trigger.getArtifactSelections().get(0).getServiceId());
    assertThat(qlLastCollected.getServiceName()).isEqualTo(trigger.getArtifactSelections().get(0).getServiceName());
    assertThat(qlLastCollected.getArtifactSourceId())
        .isEqualTo(trigger.getArtifactSelections().get(0).getArtifactStreamId());
    assertThat(qlLastCollected.getArtifactSourceName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getArtifactSourceName());
    assertThat(qlLastCollected.getArtifactFilter())
        .isEqualTo(trigger.getArtifactSelections().get(0).getArtifactFilter());
    assertThat(qlLastCollected.getRegex()).isEqualTo(trigger.getArtifactSelections().get(0).isRegex());

    assertThat(qlPipelineAction.getVariables()).isNotNull();
    assertThat(qlPipelineAction.getVariables().get(0).getValue())
        .isEqualTo(trigger.getWorkflowVariables().get("variableKey"));
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerActionShouldReturnPipelineWorkflowWitQLLastDeployedService() {
    ArtifactSelection artifactSource = ArtifactSelection.builder()
                                           .serviceId("serviceId")
                                           .serviceName("serviceName")
                                           .pipelineId("pipelineId")
                                           .pipelineName("pipelineName")
                                           .type(Type.LAST_DEPLOYED)
                                           .build();
    List<ArtifactSelection> artifactSelections = Arrays.asList(artifactSource);

    Map<String, String> variables = new HashMap<>();
    variables.put("variableKey", "variableResult");

    Trigger trigger = Trigger.builder()
                          .workflowId("workflowId")
                          .workflowName("workflowName")
                          .artifactSelections(artifactSelections)
                          .workflowVariables(variables)
                          .workflowType(WorkflowType.PIPELINE)
                          .build();

    QLPipelineAction qlPipelineAction = (QLPipelineAction) triggerActionController.populateTriggerAction(trigger);

    assertThat(qlPipelineAction).isNotNull();
    assertThat(qlPipelineAction.getPipelineId()).isEqualTo(trigger.getWorkflowId());
    assertThat(qlPipelineAction.getPipelineName()).isEqualTo(trigger.getWorkflowName());

    assertThat(qlPipelineAction.getArtifactSelections()).isNotNull();
    QLLastDeployedFromPipeline qlLastDeployedFromPipeline =
        (QLLastDeployedFromPipeline) qlPipelineAction.getArtifactSelections().get(0);
    assertThat(qlLastDeployedFromPipeline.getServiceId())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceId());
    assertThat(qlLastDeployedFromPipeline.getServiceName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceName());
    assertThat(qlLastDeployedFromPipeline.getPipelineId())
        .isEqualTo(trigger.getArtifactSelections().get(0).getPipelineId());
    assertThat(qlLastDeployedFromPipeline.getPipelineName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getPipelineName());

    assertThat(qlPipelineAction.getVariables()).isNotNull();
    assertThat(qlPipelineAction.getVariables().get(0).getValue())
        .isEqualTo(trigger.getWorkflowVariables().get("variableKey"));
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerActionShouldReturnPipelineWorkflowWithQLFromTriggeringPipelineSourceService() {
    ArtifactSelection artifactSource = ArtifactSelection.builder()
                                           .serviceId("serviceId")
                                           .serviceName("serviceName")
                                           .type(Type.PIPELINE_SOURCE)
                                           .build();
    List<ArtifactSelection> artifactSelections = Arrays.asList(artifactSource);

    Map<String, String> variables = new HashMap<>();
    variables.put("variableKey", "variableResult");

    Trigger trigger = Trigger.builder()
                          .workflowId("workflowId")
                          .workflowName("workflowName")
                          .artifactSelections(artifactSelections)
                          .workflowVariables(variables)
                          .workflowType(WorkflowType.PIPELINE)
                          .build();

    QLPipelineAction qlPipelineAction = (QLPipelineAction) triggerActionController.populateTriggerAction(trigger);

    assertThat(qlPipelineAction).isNotNull();
    assertThat(qlPipelineAction.getPipelineId()).isEqualTo(trigger.getWorkflowId());
    assertThat(qlPipelineAction.getPipelineName()).isEqualTo(trigger.getWorkflowName());

    assertThat(qlPipelineAction.getArtifactSelections()).isNotNull();
    QLFromTriggeringPipeline qlFromTriggeringPipeline =
        (QLFromTriggeringPipeline) qlPipelineAction.getArtifactSelections().get(0);
    assertThat(qlFromTriggeringPipeline.getServiceId())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceId());
    assertThat(qlFromTriggeringPipeline.getServiceName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceName());

    assertThat(qlPipelineAction.getVariables()).isNotNull();
    assertThat(qlPipelineAction.getVariables().get(0).getValue())
        .isEqualTo(trigger.getWorkflowVariables().get("variableKey"));
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerActionShouldReturnPipelineWorkflowWithQLFromWebhookPayloadService() {
    ArtifactSelection artifactSource = ArtifactSelection.builder()
                                           .serviceId("serviceId")
                                           .serviceName("serviceName")
                                           .artifactStreamId("sourceId")
                                           .artifactSourceName("sourceName")
                                           .type(Type.WEBHOOK_VARIABLE)
                                           .build();
    List<ArtifactSelection> artifactSelections = Arrays.asList(artifactSource);

    Map<String, String> variables = new HashMap<>();
    variables.put("variableKey", "variableResult");

    Trigger trigger = Trigger.builder()
                          .workflowId("workflowId")
                          .workflowName("workflowName")
                          .artifactSelections(artifactSelections)
                          .workflowVariables(variables)
                          .workflowType(WorkflowType.PIPELINE)
                          .build();

    QLPipelineAction qlPipelineAction = (QLPipelineAction) triggerActionController.populateTriggerAction(trigger);

    assertThat(qlPipelineAction).isNotNull();
    assertThat(qlPipelineAction.getPipelineId()).isEqualTo(trigger.getWorkflowId());
    assertThat(qlPipelineAction.getPipelineName()).isEqualTo(trigger.getWorkflowName());

    assertThat(qlPipelineAction.getArtifactSelections()).isNotNull();
    QLFromWebhookPayload qlFromWebhookPayload = (QLFromWebhookPayload) qlPipelineAction.getArtifactSelections().get(0);
    assertThat(qlFromWebhookPayload.getServiceId()).isEqualTo(trigger.getArtifactSelections().get(0).getServiceId());
    assertThat(qlFromWebhookPayload.getServiceName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceName());
    assertThat(qlFromWebhookPayload.getArtifactSourceId())
        .isEqualTo(trigger.getArtifactSelections().get(0).getArtifactStreamId());
    assertThat(qlFromWebhookPayload.getArtifactSourceName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getArtifactSourceName());

    assertThat(qlPipelineAction.getVariables()).isNotNull();
    assertThat(qlPipelineAction.getVariables().get(0).getValue())
        .isEqualTo(trigger.getWorkflowVariables().get("variableKey"));
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerActionShouldReturnOrchestrationWorkflowWithFromTriggeringArtifactSourceService() {
    ArtifactSelection artifactSource = ArtifactSelection.builder()
                                           .serviceId("serviceId")
                                           .serviceName("serviceName")
                                           .type(Type.ARTIFACT_SOURCE)
                                           .build();
    List<ArtifactSelection> artifactSelections = Arrays.asList(artifactSource);
    Map<String, String> variables = new HashMap<>();
    variables.put("variableKey", "variableResult");
    Trigger trigger = Trigger.builder()
                          .workflowType(WorkflowType.ORCHESTRATION)
                          .workflowId("workflowId")
                          .workflowName("workflowName")
                          .artifactSelections(artifactSelections)
                          .workflowVariables(variables)
                          .build();

    QLWorkflowAction qlWorkflowAction = (QLWorkflowAction) triggerActionController.populateTriggerAction(trigger);

    assertThat(qlWorkflowAction).isNotNull();
    assertThat(qlWorkflowAction.getWorkflowId()).isEqualTo(trigger.getWorkflowId());
    assertThat(qlWorkflowAction.getWorkflowName()).isEqualTo(trigger.getWorkflowName());

    assertThat(qlWorkflowAction.getArtifactSelections()).isNotNull();
    assertThat(qlWorkflowAction.getArtifactSelections().get(0).getServiceId())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceId());
    assertThat(qlWorkflowAction.getArtifactSelections().get(0).getServiceName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceName());

    assertThat(qlWorkflowAction.getVariables()).isNotNull();
    assertThat(qlWorkflowAction.getVariables().get(0).getValue())
        .isEqualTo(trigger.getWorkflowVariables().get("variableKey"));
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerActionShouldReturnOrchestrationWorkflowWitQLLastCollectedService() {
    ArtifactSelection artifactSource = ArtifactSelection.builder()
                                           .serviceId("serviceId")
                                           .serviceName("serviceName")
                                           .artifactStreamId("streamId")
                                           .artifactSourceName("sourceName")
                                           .artifactFilter("filter")
                                           .regex(true)
                                           .type(Type.LAST_COLLECTED)
                                           .build();
    List<ArtifactSelection> artifactSelections = Arrays.asList(artifactSource);

    Map<String, String> variables = new HashMap<>();
    variables.put("variableKey", "variableResult");
    Trigger trigger = Trigger.builder()
                          .workflowId("workflowId")
                          .workflowName("workflowName")
                          .artifactSelections(artifactSelections)
                          .workflowVariables(variables)
                          .workflowType(WorkflowType.ORCHESTRATION)
                          .build();

    QLWorkflowAction qlWorkflowAction = (QLWorkflowAction) triggerActionController.populateTriggerAction(trigger);

    assertThat(qlWorkflowAction).isNotNull();
    assertThat(qlWorkflowAction.getWorkflowId()).isEqualTo(trigger.getWorkflowId());
    assertThat(qlWorkflowAction.getWorkflowName()).isEqualTo(trigger.getWorkflowName());

    assertThat(qlWorkflowAction.getArtifactSelections()).isNotNull();
    QLLastCollected qlLastCollected = (QLLastCollected) qlWorkflowAction.getArtifactSelections().get(0);
    assertThat(qlLastCollected.getServiceId()).isEqualTo(trigger.getArtifactSelections().get(0).getServiceId());
    assertThat(qlLastCollected.getServiceName()).isEqualTo(trigger.getArtifactSelections().get(0).getServiceName());
    assertThat(qlLastCollected.getArtifactSourceId())
        .isEqualTo(trigger.getArtifactSelections().get(0).getArtifactStreamId());
    assertThat(qlLastCollected.getArtifactSourceName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getArtifactSourceName());
    assertThat(qlLastCollected.getArtifactFilter())
        .isEqualTo(trigger.getArtifactSelections().get(0).getArtifactFilter());
    assertThat(qlLastCollected.getRegex()).isEqualTo(trigger.getArtifactSelections().get(0).isRegex());

    assertThat(qlWorkflowAction.getVariables()).isNotNull();
    assertThat(qlWorkflowAction.getVariables().get(0).getValue())
        .isEqualTo(trigger.getWorkflowVariables().get("variableKey"));
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerActionShouldReturnOrchestrationWorkflowWitQLLastDeployedService() {
    ArtifactSelection artifactSource = ArtifactSelection.builder()
                                           .serviceId("serviceId")
                                           .serviceName("serviceName")
                                           .workflowId("pipelineId")
                                           .workflowName("pipelineName")
                                           .type(Type.LAST_DEPLOYED)
                                           .build();
    List<ArtifactSelection> artifactSelections = Arrays.asList(artifactSource);

    Map<String, String> variables = new HashMap<>();
    variables.put("variableKey", "variableResult");

    Trigger trigger = Trigger.builder()
                          .workflowId("workflowId")
                          .workflowName("workflowName")
                          .artifactSelections(artifactSelections)
                          .workflowVariables(variables)
                          .workflowType(WorkflowType.ORCHESTRATION)
                          .build();

    QLWorkflowAction qlWorkflowAction = (QLWorkflowAction) triggerActionController.populateTriggerAction(trigger);

    assertThat(qlWorkflowAction).isNotNull();
    assertThat(qlWorkflowAction.getWorkflowId()).isEqualTo(trigger.getWorkflowId());
    assertThat(qlWorkflowAction.getWorkflowName()).isEqualTo(trigger.getWorkflowName());

    assertThat(qlWorkflowAction.getArtifactSelections()).isNotNull();
    QLLastDeployedFromWorkflow qlLastDeployedFromWorkflow =
        (QLLastDeployedFromWorkflow) qlWorkflowAction.getArtifactSelections().get(0);
    assertThat(qlLastDeployedFromWorkflow.getServiceId())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceId());
    assertThat(qlLastDeployedFromWorkflow.getServiceName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceName());
    assertThat(qlLastDeployedFromWorkflow.getWorkflowId())
        .isEqualTo(trigger.getArtifactSelections().get(0).getWorkflowId());
    assertThat(qlLastDeployedFromWorkflow.getWorkflowName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getWorkflowName());

    assertThat(qlWorkflowAction.getVariables()).isNotNull();
    assertThat(qlWorkflowAction.getVariables().get(0).getValue())
        .isEqualTo(trigger.getWorkflowVariables().get("variableKey"));
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerActionShouldReturnOrchestrationWorkflowWithQLFromTriggeringPipelineSourceService() {
    ArtifactSelection artifactSource = ArtifactSelection.builder()
                                           .serviceId("serviceId")
                                           .serviceName("serviceName")
                                           .type(Type.PIPELINE_SOURCE)
                                           .build();
    List<ArtifactSelection> artifactSelections = Arrays.asList(artifactSource);

    Map<String, String> variables = new HashMap<>();
    variables.put("variableKey", "variableResult");

    Trigger trigger = Trigger.builder()
                          .workflowId("workflowId")
                          .workflowName("workflowName")
                          .artifactSelections(artifactSelections)
                          .workflowVariables(variables)
                          .workflowType(WorkflowType.ORCHESTRATION)
                          .build();

    QLWorkflowAction qlWorkflowAction = (QLWorkflowAction) triggerActionController.populateTriggerAction(trigger);

    assertThat(qlWorkflowAction).isNotNull();
    assertThat(qlWorkflowAction.getWorkflowId()).isEqualTo(trigger.getWorkflowId());
    assertThat(qlWorkflowAction.getWorkflowName()).isEqualTo(trigger.getWorkflowName());

    assertThat(qlWorkflowAction.getArtifactSelections()).isNotNull();
    QLFromTriggeringPipeline qlFromTriggeringPipeline =
        (QLFromTriggeringPipeline) qlWorkflowAction.getArtifactSelections().get(0);
    assertThat(qlFromTriggeringPipeline.getServiceId())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceId());
    assertThat(qlFromTriggeringPipeline.getServiceName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceName());

    assertThat(qlWorkflowAction.getVariables()).isNotNull();
    assertThat(qlWorkflowAction.getVariables().get(0).getValue())
        .isEqualTo(trigger.getWorkflowVariables().get("variableKey"));
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void populateTriggerActionShouldReturnOrchestrationWorkflowWithQLFromWebhookPayloadService() {
    ArtifactSelection artifactSource = ArtifactSelection.builder()
                                           .serviceId("serviceId")
                                           .serviceName("serviceName")
                                           .artifactStreamId("sourceId")
                                           .artifactSourceName("sourceName")
                                           .type(Type.WEBHOOK_VARIABLE)
                                           .build();
    List<ArtifactSelection> artifactSelections = Arrays.asList(artifactSource);

    Map<String, String> variables = new HashMap<>();
    variables.put("variableKey", "variableResult");

    Trigger trigger = Trigger.builder()
                          .workflowId("workflowId")
                          .workflowName("workflowName")
                          .artifactSelections(artifactSelections)
                          .workflowVariables(variables)
                          .workflowType(WorkflowType.ORCHESTRATION)
                          .build();

    QLWorkflowAction qlWorkflowAction = (QLWorkflowAction) triggerActionController.populateTriggerAction(trigger);

    assertThat(qlWorkflowAction).isNotNull();
    assertThat(qlWorkflowAction.getWorkflowId()).isEqualTo(trigger.getWorkflowId());
    assertThat(qlWorkflowAction.getWorkflowName()).isEqualTo(trigger.getWorkflowName());

    assertThat(qlWorkflowAction.getArtifactSelections()).isNotNull();
    QLFromWebhookPayload qlFromWebhookPayload = (QLFromWebhookPayload) qlWorkflowAction.getArtifactSelections().get(0);
    assertThat(qlFromWebhookPayload.getServiceId()).isEqualTo(trigger.getArtifactSelections().get(0).getServiceId());
    assertThat(qlFromWebhookPayload.getServiceName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getServiceName());
    assertThat(qlFromWebhookPayload.getArtifactSourceId())
        .isEqualTo(trigger.getArtifactSelections().get(0).getArtifactStreamId());
    assertThat(qlFromWebhookPayload.getArtifactSourceName())
        .isEqualTo(trigger.getArtifactSelections().get(0).getArtifactSourceName());

    assertThat(qlWorkflowAction.getVariables()).isNotNull();
    assertThat(qlWorkflowAction.getVariables().get(0).getValue())
        .isEqualTo(trigger.getWorkflowVariables().get("variableKey"));
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveArtifactSelectionsShouldThrowEmptyServiceIdException() throws Exception {
    QLArtifactSelectionInput qlArtifactSelectionInput =
        QLArtifactSelectionInput.builder()
            .artifactSelectionType(QLArtifactSelectionType.FROM_TRIGGERING_ARTIFACT)
            .serviceId("S1")
            .build();

    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder()
            .action(QLTriggerActionInput.builder().artifactSelections(Arrays.asList(qlArtifactSelectionInput)).build())
            .build();
    triggerActionController.resolveArtifactSelections(qlCreateOrUpdateTriggerInput, Collections.singletonList("S1"));
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveArtifactSelectionsShouldThrowServiceDoesNotExistException() throws Exception {
    QLArtifactSelectionInput qlArtifactSelectionInput =
        QLArtifactSelectionInput.builder()
            .artifactSelectionType(QLArtifactSelectionType.FROM_TRIGGERING_ARTIFACT)
            .serviceId("serviceId")
            .build();

    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder()
            .action(QLTriggerActionInput.builder().artifactSelections(Arrays.asList(qlArtifactSelectionInput)).build())
            .build();

    Mockito.when(serviceResourceService.get(Matchers.anyString())).thenReturn(null);

    triggerActionController.resolveArtifactSelections(qlCreateOrUpdateTriggerInput, Collections.singletonList("S1"));
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveArtifactSelectionsShouldReturnEmptyList() {
    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder()
            .action(QLTriggerActionInput.builder().artifactSelections(null).build())
            .build();

    List<ArtifactSelection> artifactSelections = triggerActionController.resolveArtifactSelections(
        qlCreateOrUpdateTriggerInput, Collections.singletonList("S1"));

    assertThat(artifactSelections).isEmpty();
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveArtifactSelectionsShouldReturnOnTriggeringArtifactArtifactSelectionType() throws Exception {
    QLArtifactSelectionInput qlArtifactSelectionInput =
        QLArtifactSelectionInput.builder()
            .artifactSelectionType(QLArtifactSelectionType.FROM_TRIGGERING_ARTIFACT)
            .serviceId("serviceId")
            .artifactSourceId("artifactSourceId")
            .artifactFilter("filter")
            .regex(true)
            .pipelineId("pipelineId")
            .workflowId("workflowId")
            .build();

    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder()
            .action(QLTriggerActionInput.builder().artifactSelections(Arrays.asList(qlArtifactSelectionInput)).build())
            .build();

    Mockito.doReturn(Type.ARTIFACT_SOURCE)
        .when(triggerActionController)
        .validateAndResolveFromTriggeringArtifactArtifactSelectionType(
            Matchers.any(QLCreateOrUpdateTriggerInput.class));

    Mockito.when(serviceResourceService.get(Matchers.anyString())).thenReturn(Mockito.mock(Service.class));

    ArtifactSelection returnedArtifactSelection =
        triggerActionController.resolveArtifactSelections(qlCreateOrUpdateTriggerInput, Collections.singletonList("S1"))
            .get(0);

    assertThat(returnedArtifactSelection).isNotNull();
    assertThat(returnedArtifactSelection.getType()).isEqualByComparingTo(ArtifactSelection.Type.ARTIFACT_SOURCE);
    assertThat(returnedArtifactSelection.getServiceId()).isEqualTo(qlArtifactSelectionInput.getServiceId());
    assertThat(returnedArtifactSelection.getArtifactStreamId())
        .isEqualTo(qlArtifactSelectionInput.getArtifactSourceId());
    assertThat(returnedArtifactSelection.getArtifactFilter()).isEqualTo(qlArtifactSelectionInput.getArtifactFilter());
    assertThat(returnedArtifactSelection.isRegex()).isEqualTo(qlArtifactSelectionInput.getRegex());
    assertThat(returnedArtifactSelection.getPipelineId()).isEqualTo(qlArtifactSelectionInput.getPipelineId());
    assertThat(returnedArtifactSelection.getWorkflowId()).isEqualTo(qlArtifactSelectionInput.getWorkflowId());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveArtifactSelectionsShouldReturnOnTriggeringPipelineArtifactSelectionType() throws Exception {
    QLArtifactSelectionInput qlArtifactSelectionInput =
        QLArtifactSelectionInput.builder()
            .artifactSelectionType(QLArtifactSelectionType.FROM_TRIGGERING_PIPELINE)
            .serviceId("serviceId")
            .artifactSourceId("artifactSourceId")
            .artifactFilter("filter")
            .regex(true)
            .pipelineId("pipelineId")
            .workflowId("workflowId")
            .build();

    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder()
            .action(QLTriggerActionInput.builder().artifactSelections(Arrays.asList(qlArtifactSelectionInput)).build())
            .build();

    Mockito.doReturn(Type.PIPELINE_SOURCE)
        .when(triggerActionController)
        .validateAndResolveFromTriggeringPipelineArtifactSelectionType(
            Matchers.any(QLCreateOrUpdateTriggerInput.class));

    Mockito.when(serviceResourceService.get(Matchers.anyString())).thenReturn(Mockito.mock(Service.class));

    ArtifactSelection returnedArtifactSelection =
        triggerActionController.resolveArtifactSelections(qlCreateOrUpdateTriggerInput, Collections.singletonList("S1"))
            .get(0);

    assertThat(returnedArtifactSelection).isNotNull();
    assertThat(returnedArtifactSelection.getType()).isEqualByComparingTo(ArtifactSelection.Type.PIPELINE_SOURCE);
    assertThat(returnedArtifactSelection.getServiceId()).isEqualTo(qlArtifactSelectionInput.getServiceId());
    assertThat(returnedArtifactSelection.getArtifactStreamId())
        .isEqualTo(qlArtifactSelectionInput.getArtifactSourceId());
    assertThat(returnedArtifactSelection.getArtifactFilter()).isEqualTo(qlArtifactSelectionInput.getArtifactFilter());
    assertThat(returnedArtifactSelection.isRegex()).isEqualTo(qlArtifactSelectionInput.getRegex());
    assertThat(returnedArtifactSelection.getPipelineId()).isEqualTo(qlArtifactSelectionInput.getPipelineId());
    assertThat(returnedArtifactSelection.getWorkflowId()).isEqualTo(qlArtifactSelectionInput.getWorkflowId());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveArtifactSelectionsShouldReturnLastCollectedArtifactSelectionType() throws Exception {
    QLArtifactSelectionInput qlArtifactSelectionInput =
        QLArtifactSelectionInput.builder()
            .artifactSelectionType(QLArtifactSelectionType.LAST_COLLECTED)
            .serviceId("serviceId")
            .artifactSourceId("artifactSourceId")
            .artifactFilter("filter")
            .regex(true)
            .pipelineId("pipelineId")
            .workflowId("workflowId")
            .build();

    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder()
            .action(QLTriggerActionInput.builder().artifactSelections(Arrays.asList(qlArtifactSelectionInput)).build())
            .build();

    Mockito.doReturn(Type.LAST_COLLECTED)
        .when(triggerActionController)
        .validateAndResolveLastCollectedArtifactSelectionType(Matchers.any(QLArtifactSelectionInput.class));

    Mockito.when(serviceResourceService.get(Matchers.anyString())).thenReturn(Mockito.mock(Service.class));

    ArtifactSelection returnedArtifactSelection =
        triggerActionController.resolveArtifactSelections(qlCreateOrUpdateTriggerInput, Collections.singletonList("S1"))
            .get(0);

    assertThat(returnedArtifactSelection).isNotNull();
    assertThat(returnedArtifactSelection.getType()).isEqualByComparingTo(ArtifactSelection.Type.LAST_COLLECTED);
    assertThat(returnedArtifactSelection.getServiceId()).isEqualTo(qlArtifactSelectionInput.getServiceId());
    assertThat(returnedArtifactSelection.getArtifactStreamId())
        .isEqualTo(qlArtifactSelectionInput.getArtifactSourceId());
    assertThat(returnedArtifactSelection.getArtifactFilter()).isEqualTo(qlArtifactSelectionInput.getArtifactFilter());
    assertThat(returnedArtifactSelection.isRegex()).isEqualTo(qlArtifactSelectionInput.getRegex());
    assertThat(returnedArtifactSelection.getPipelineId()).isEqualTo(qlArtifactSelectionInput.getPipelineId());
    assertThat(returnedArtifactSelection.getWorkflowId()).isEqualTo(qlArtifactSelectionInput.getWorkflowId());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveArtifactSelectionsShouldReturnFromPayloadSourceArtifactSelectionType() throws Exception {
    QLArtifactSelectionInput qlArtifactSelectionInput =
        QLArtifactSelectionInput.builder()
            .artifactSelectionType(QLArtifactSelectionType.FROM_PAYLOAD_SOURCE)
            .serviceId("serviceId")
            .artifactSourceId("artifactSourceId")
            .artifactFilter("filter")
            .regex(true)
            .pipelineId("pipelineId")
            .workflowId("workflowId")
            .build();

    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder()
            .action(QLTriggerActionInput.builder().artifactSelections(Arrays.asList(qlArtifactSelectionInput)).build())
            .build();

    Mockito.doReturn(Type.WEBHOOK_VARIABLE)
        .when(triggerActionController)
        .validateAndResolveFromPayloadSourceArtifactSelectionType(
            Matchers.any(QLCreateOrUpdateTriggerInput.class), Matchers.any(QLArtifactSelectionInput.class));

    Mockito.when(serviceResourceService.get(Matchers.anyString())).thenReturn(Mockito.mock(Service.class));

    ArtifactSelection returnedArtifactSelection =
        triggerActionController.resolveArtifactSelections(qlCreateOrUpdateTriggerInput, Collections.singletonList("S1"))
            .get(0);

    assertThat(returnedArtifactSelection).isNotNull();
    assertThat(returnedArtifactSelection.getType()).isEqualByComparingTo(ArtifactSelection.Type.WEBHOOK_VARIABLE);
    assertThat(returnedArtifactSelection.getServiceId()).isEqualTo(qlArtifactSelectionInput.getServiceId());
    assertThat(returnedArtifactSelection.getArtifactStreamId())
        .isEqualTo(qlArtifactSelectionInput.getArtifactSourceId());
    assertThat(returnedArtifactSelection.getArtifactFilter()).isEqualTo(qlArtifactSelectionInput.getArtifactFilter());
    assertThat(returnedArtifactSelection.isRegex()).isEqualTo(qlArtifactSelectionInput.getRegex());
    assertThat(returnedArtifactSelection.getPipelineId()).isEqualTo(qlArtifactSelectionInput.getPipelineId());
    assertThat(returnedArtifactSelection.getWorkflowId()).isEqualTo(qlArtifactSelectionInput.getWorkflowId());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveArtifactSelectionsShouldReturnLastDeployedPipelineArtifactSelectionType() throws Exception {
    QLArtifactSelectionInput qlArtifactSelectionInput =
        QLArtifactSelectionInput.builder()
            .artifactSelectionType(QLArtifactSelectionType.LAST_DEPLOYED_PIPELINE)
            .serviceId("serviceId")
            .artifactSourceId("artifactSourceId")
            .artifactFilter("filter")
            .regex(true)
            .pipelineId("pipelineId")
            .workflowId("workflowId")
            .build();

    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder()
            .action(QLTriggerActionInput.builder().artifactSelections(Arrays.asList(qlArtifactSelectionInput)).build())
            .build();

    Mockito.doReturn(Type.LAST_DEPLOYED)
        .when(triggerActionController)
        .validateAndResolveLastDeployedPipelineArtifactSelectionType(Matchers.any(QLCreateOrUpdateTriggerInput.class));

    Mockito.when(serviceResourceService.get(Matchers.anyString())).thenReturn(Mockito.mock(Service.class));

    ArtifactSelection returnedArtifactSelection =
        triggerActionController.resolveArtifactSelections(qlCreateOrUpdateTriggerInput, Collections.singletonList("S1"))
            .get(0);

    assertThat(returnedArtifactSelection).isNotNull();
    assertThat(returnedArtifactSelection.getType()).isEqualByComparingTo(ArtifactSelection.Type.LAST_DEPLOYED);
    assertThat(returnedArtifactSelection.getServiceId()).isEqualTo(qlArtifactSelectionInput.getServiceId());
    assertThat(returnedArtifactSelection.getArtifactStreamId())
        .isEqualTo(qlArtifactSelectionInput.getArtifactSourceId());
    assertThat(returnedArtifactSelection.getArtifactFilter()).isEqualTo(qlArtifactSelectionInput.getArtifactFilter());
    assertThat(returnedArtifactSelection.isRegex()).isEqualTo(qlArtifactSelectionInput.getRegex());
    assertThat(returnedArtifactSelection.getPipelineId()).isEqualTo(qlArtifactSelectionInput.getPipelineId());
    assertThat(returnedArtifactSelection.getWorkflowId()).isEqualTo(qlArtifactSelectionInput.getWorkflowId());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveArtifactSelectionsShouldReturnLastDeployedWorkflowArtifactSelectionType() throws Exception {
    QLArtifactSelectionInput qlArtifactSelectionInput =
        QLArtifactSelectionInput.builder()
            .artifactSelectionType(QLArtifactSelectionType.LAST_DEPLOYED_WORKFLOW)
            .serviceId("serviceId")
            .artifactSourceId("artifactSourceId")
            .artifactFilter("filter")
            .regex(true)
            .pipelineId("pipelineId")
            .workflowId("workflowId")
            .build();

    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder()
            .action(QLTriggerActionInput.builder().artifactSelections(Arrays.asList(qlArtifactSelectionInput)).build())
            .build();

    Mockito.doReturn(Type.LAST_DEPLOYED)
        .when(triggerActionController)
        .validateAndResolveLastDeployedWorkflowArtifactSelectionType(Matchers.any(QLCreateOrUpdateTriggerInput.class));

    Mockito.when(serviceResourceService.get(Matchers.anyString())).thenReturn(Mockito.mock(Service.class));

    ArtifactSelection returnedArtifactSelection =
        triggerActionController.resolveArtifactSelections(qlCreateOrUpdateTriggerInput, Collections.singletonList("S1"))
            .get(0);

    assertThat(returnedArtifactSelection).isNotNull();
    assertThat(returnedArtifactSelection.getType()).isEqualByComparingTo(ArtifactSelection.Type.LAST_DEPLOYED);
    assertThat(returnedArtifactSelection.getServiceId()).isEqualTo(qlArtifactSelectionInput.getServiceId());
    assertThat(returnedArtifactSelection.getArtifactStreamId())
        .isEqualTo(qlArtifactSelectionInput.getArtifactSourceId());
    assertThat(returnedArtifactSelection.getArtifactFilter()).isEqualTo(qlArtifactSelectionInput.getArtifactFilter());
    assertThat(returnedArtifactSelection.isRegex()).isEqualTo(qlArtifactSelectionInput.getRegex());
    assertThat(returnedArtifactSelection.getPipelineId()).isEqualTo(qlArtifactSelectionInput.getPipelineId());
    assertThat(returnedArtifactSelection.getWorkflowId()).isEqualTo(qlArtifactSelectionInput.getWorkflowId());
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveWorkflowTypeShouldReturnOrchestrationWorkflowType() {
    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder()
            .action(QLTriggerActionInput.builder().executionType(QLExecutionType.WORKFLOW).build())
            .build();

    assertThat(triggerActionController.resolveWorkflowType(qlCreateOrUpdateTriggerInput))
        .isEqualByComparingTo(WorkflowType.ORCHESTRATION);
  }

  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void resolveWorkflowTypeShouldReturnPipelineWorkflowType() {
    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder()
            .action(QLTriggerActionInput.builder().executionType(QLExecutionType.PIPELINE).build())
            .build();

    assertThat(triggerActionController.resolveWorkflowType(qlCreateOrUpdateTriggerInput))
        .isEqualByComparingTo(WorkflowType.PIPELINE);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void validatePipelineShouldThrowAppIdMustNotBeEmptyException() {
    QLTriggerConditionInput qlTriggerConditionInput = QLTriggerConditionInput.builder().build();
    QLTriggerActionInput qlTriggerActionInput = QLTriggerActionInput.builder().entityId("entityId").build();
    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput = QLCreateOrUpdateTriggerInput.builder()
                                                                    .condition(qlTriggerConditionInput)
                                                                    .action(qlTriggerActionInput)
                                                                    .applicationId(null)
                                                                    .build();

    triggerActionController.validatePipeline(qlCreateOrUpdateTriggerInput, "pipelineId");
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void validatePipelineShouldThrowPipelineIdMustNotBeEmptyException() {
    QLTriggerActionInput qlTriggerActionInput = QLTriggerActionInput.builder().entityId(null).build();
    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder().action(qlTriggerActionInput).applicationId("appId").build();

    triggerActionController.validatePipeline(qlCreateOrUpdateTriggerInput, "pipelineId");
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void validatePipelineShouldThrowPipelineDoesntExistException() {
    QLTriggerActionInput qlTriggerActionInput = QLTriggerActionInput.builder().entityId("entityId").build();
    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder().action(qlTriggerActionInput).applicationId("appId").build();

    Mockito.when(pipelineService.readPipeline(Matchers.anyString(), Matchers.anyString(), Matchers.anyBoolean()))
        .thenReturn(null);

    triggerActionController.validatePipeline(qlCreateOrUpdateTriggerInput, "pipelineId");
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void validatePipelineShouldThrowPipelineDoesntBelongToThisAppException() {
    QLTriggerActionInput qlTriggerActionInput = QLTriggerActionInput.builder().entityId("entityId").build();
    QLCreateOrUpdateTriggerInput qlCreateOrUpdateTriggerInput =
        QLCreateOrUpdateTriggerInput.builder().action(qlTriggerActionInput).applicationId("appId").build();

    Mockito.when(pipelineService.readPipeline(Matchers.anyString(), Matchers.anyString(), Matchers.anyBoolean()))
        .thenReturn(Pipeline.builder().appId("appId2").build());

    triggerActionController.validatePipeline(qlCreateOrUpdateTriggerInput, "pipelineId");
  }
}
