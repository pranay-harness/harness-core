package software.wings.service.impl.trigger;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PipelineStage.PipelineStageElement;
import static software.wings.beans.PipelineStage.builder;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILTER;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID_CHANGED;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_NAME;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.Service;
import software.wings.beans.Variable;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.NewInstanceTriggerCondition;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.ServiceInfraWorkflow;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.common.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerServiceTestHelper {
  public static Artifact artifact = anArtifact()
                                        .withAppId(APP_ID)
                                        .withUuid(ARTIFACT_ID)
                                        .withArtifactStreamId(ARTIFACT_STREAM_ID)
                                        .withServiceIds(singletonList(SERVICE_ID))
                                        .withMetadata(ImmutableMap.of("buildNo", ARTIFACT_FILTER))
                                        .build();

  public static Trigger buildArtifactTrigger() {
    return Trigger.builder()
        .workflowId(PIPELINE_ID)
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(ArtifactTriggerCondition.builder()
                       .artifactFilter(ARTIFACT_FILTER)
                       .artifactStreamId(ARTIFACT_STREAM_ID)
                       .build())
        .build();
  }

  public static Trigger buildWorkflowArtifactTrigger() {
    return Trigger.builder()
        .workflowId(WORKFLOW_ID)
        .uuid(TRIGGER_ID)
        .workflowType(ORCHESTRATION)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(ArtifactTriggerCondition.builder()
                       .artifactFilter(ARTIFACT_FILTER)
                       .artifactStreamId(ARTIFACT_STREAM_ID)
                       .build())
        .build();
  }

  public static Trigger buildPipelineCondTrigger() {
    return Trigger.builder()
        .workflowId(PIPELINE_ID)
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(PipelineTriggerCondition.builder().pipelineId(PIPELINE_ID).build())
        .build();
  }

  public static Trigger buildScheduledCondTrigger() {
    return Trigger.builder()
        .workflowId(PIPELINE_ID)
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(ScheduledTriggerCondition.builder().cronExpression("* * * * ?").build())
        .build();
  }

  public static Trigger buildWebhookCondTrigger() {
    return Trigger.builder()
        .workflowId(PIPELINE_ID)
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(WebHookTriggerCondition.builder()
                       .webHookToken(WebHookToken.builder().webHookToken(generateUuid()).build())
                       .build())
        .build();
  }

  public static Trigger buildNewInstanceTrigger() {
    return Trigger.builder()
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .workflowType(ORCHESTRATION)
        .workflowId(WORKFLOW_ID)
        .name("New Instance Trigger")
        .serviceInfraWorkflows(
            asList(ServiceInfraWorkflow.builder().infraMappingId(INFRA_MAPPING_ID).workflowId(WORKFLOW_ID).build()))
        .condition(NewInstanceTriggerCondition.builder().build())
        .build();
  }

  public static Pipeline buildPipeline() {
    return Pipeline.builder()
        .appId(APP_ID)
        .uuid(PIPELINE_ID)
        .services(asList(Service.builder().uuid(SERVICE_ID).name("Catalog").build(),
            Service.builder().uuid(SERVICE_ID_CHANGED).name("Order").build()))
        .build();
  }

  public static Trigger buildWorkflowWebhookTrigger() {
    return Trigger.builder()
        .workflowId(WORKFLOW_ID)
        .workflowType(ORCHESTRATION)
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(WebHookTriggerCondition.builder()
                       .webHookToken(WebHookToken.builder().build())
                       .parameters(ImmutableMap.of("MyVar", "MyVal"))
                       .build())
        .build();
  }

  public static Workflow buildWorkflow() {
    List<Variable> userVariables = new ArrayList<>();
    userVariables.add(aVariable().withName("MyVar").withValue("MyVal").build());
    return aWorkflow()
        .withEnvId(ENV_ID)
        .withName(WORKFLOW_NAME)
        .withAppId(APP_ID)
        .withServiceId(SERVICE_ID)
        .withInfraMappingId(INFRA_MAPPING_ID)
        .withOrchestrationWorkflow(
            aBasicOrchestrationWorkflow()
                .withUserVariables(userVariables)
                .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).build())
                .build())
        .withServices(asList(Service.builder().uuid(SERVICE_ID).name("Catalog").build(),
            Service.builder().uuid(SERVICE_ID_CHANGED).name("Order").build()))
        .build();
  }

  public static JenkinsArtifactStream buildJenkinsArtifactStream() {
    return JenkinsArtifactStream.builder()
        .appId(APP_ID)
        .uuid(ARTIFACT_STREAM_ID)
        .sourceName(ARTIFACT_SOURCE_NAME)
        .settingId(SETTING_ID)
        .jobname("JOB")
        .serviceId(SERVICE_ID)
        .artifactPaths(asList("*WAR"))
        .build();
  }

  public static void setPipelineStages(Pipeline pipeline) {
    Map<String, Object> properties = new HashMap<>();
    properties.put("envId", ENV_ID);
    properties.put("workflowId", WORKFLOW_ID);

    List<PipelineStage> pipelineStages = new ArrayList<>();
    PipelineStage pipelineStage =
        builder()
            .pipelineStageElements(asList(PipelineStageElement.builder()
                                              .name("STAGE 1")
                                              .type(ENV_STATE.name())
                                              .properties(properties)
                                              .workflowVariables(ImmutableMap.of("Environment", ENV_ID, "Service",
                                                  SERVICE_ID, "ServiceInfraStructure", INFRA_MAPPING_ID))
                                              .build()))
            .build();
    pipelineStages.add(pipelineStage);
    pipeline.setPipelineStages(pipelineStages);
  }

  public static void assertWebhookToken(Trigger savedTrigger) {
    assertThat(savedTrigger.getUuid()).isEqualTo(TRIGGER_ID);
    assertThat(savedTrigger.getCondition()).isInstanceOf(WebHookTriggerCondition.class);
    WebHookToken webHookToken = ((WebHookTriggerCondition) savedTrigger.getCondition()).getWebHookToken();
    assertThat(webHookToken).isNotNull();
    assertThat(webHookToken.getWebHookToken()).isNotNull();
    assertThat(webHookToken.getPayload()).isNotEmpty();

    HashMap<String, Object> hashMap = new Gson().fromJson(webHookToken.getPayload(), HashMap.class);
    assertThat(hashMap).containsKeys("application", "artifacts", "parameters");
    assertThat(hashMap.get("application")).isEqualTo(APP_ID);
    assertThat(hashMap.get("artifacts"))
        .isNotNull()
        .toString()
        .contains(
            "{service=Catalog, buildNumber=Catalog_BUILD_NUMBER_PLACE_HOLDER}, {service=Order, buildNumber=Order_BUILD_NUMBER_PLACE_HOLDER}");
    assertThat(hashMap.get("parameters")).isNotNull().toString().contains("MyVar=MyVar_placeholder");
  }
}
