package software.wings.graphql.datafetcher.trigger;

import static io.harness.rule.OwnerRule.MILAN;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.trigger.ArtifactSelection;
import software.wings.beans.trigger.ArtifactSelection.Type;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.schema.type.trigger.QLFromTriggeringPipeline;
import software.wings.graphql.schema.type.trigger.QLFromWebhookPayload;
import software.wings.graphql.schema.type.trigger.QLLastCollected;
import software.wings.graphql.schema.type.trigger.QLLastDeployedFromPipeline;
import software.wings.graphql.schema.type.trigger.QLLastDeployedFromWorkflow;
import software.wings.graphql.schema.type.trigger.QLPipelineAction;
import software.wings.graphql.schema.type.trigger.QLWorkflowAction;
import software.wings.graphql.schema.type.trigger.TriggerActionController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerActionControllerTest {
  @Test
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void shouldReturnPipelineWorkflowWithFromTriggeringArtifactSourceService() {
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

    QLPipelineAction qlPipelineAction = (QLPipelineAction) TriggerActionController.populateTriggerAction(trigger);

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
  public void shouldReturnPipelineWorkflowWitQLLastCollectedService() {
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

    QLPipelineAction qlPipelineAction = (QLPipelineAction) TriggerActionController.populateTriggerAction(trigger);

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
  public void shouldReturnPipelineWorkflowWitQLLastDeployedService() {
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

    QLPipelineAction qlPipelineAction = (QLPipelineAction) TriggerActionController.populateTriggerAction(trigger);

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
  public void shouldReturnPipelineWorkflowWithQLFromTriggeringPipelineSourceService() {
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

    QLPipelineAction qlPipelineAction = (QLPipelineAction) TriggerActionController.populateTriggerAction(trigger);

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
  public void shouldReturnPipelineWorkflowWithQLFromWebhookPayloadService() {
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

    QLPipelineAction qlPipelineAction = (QLPipelineAction) TriggerActionController.populateTriggerAction(trigger);

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
  public void shouldReturnOrchestrationWorkflowWithFromTriggeringArtifactSourceService() {
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

    QLWorkflowAction qlWorkflowAction = (QLWorkflowAction) TriggerActionController.populateTriggerAction(trigger);

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
  public void shouldReturnOrchestrationWorkflowWitQLLastCollectedService() {
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

    QLWorkflowAction qlWorkflowAction = (QLWorkflowAction) TriggerActionController.populateTriggerAction(trigger);

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
  public void shouldReturnOrchestrationWorkflowWitQLLastDeployedService() {
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

    QLWorkflowAction qlWorkflowAction = (QLWorkflowAction) TriggerActionController.populateTriggerAction(trigger);

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
  public void shouldReturnOrchestrationWorkflowWithQLFromTriggeringPipelineSourceService() {
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

    QLWorkflowAction qlWorkflowAction = (QLWorkflowAction) TriggerActionController.populateTriggerAction(trigger);

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
  public void shouldReturnOrchestrationWorkflowWithQLFromWebhookPayloadService() {
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

    QLWorkflowAction qlWorkflowAction = (QLWorkflowAction) TriggerActionController.populateTriggerAction(trigger);

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
}
