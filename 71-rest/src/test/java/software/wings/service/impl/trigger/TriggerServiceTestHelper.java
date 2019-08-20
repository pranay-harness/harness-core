package software.wings.service.impl.trigger;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static software.wings.beans.BasicOrchestrationWorkflow.BasicOrchestrationWorkflowBuilder.aBasicOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.PipelineStage.PipelineStageElement;
import static software.wings.beans.PipelineStage.builder;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.artifact.Artifact.Builder.anArtifact;
import static software.wings.sm.StateType.ENV_STATE;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_FILTER;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.ENTITY_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.PIPELINE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID_CHANGED;
import static software.wings.utils.WingsTestConstants.SETTING_ID;
import static software.wings.utils.WingsTestConstants.SETTING_NAME;
import static software.wings.utils.WingsTestConstants.TRIGGER_ID;
import static software.wings.utils.WingsTestConstants.TRIGGER_NAME;
import static software.wings.utils.WingsTestConstants.VARIABLE_NAME;
import static software.wings.utils.WingsTestConstants.VARIABLE_VALUE;
import static software.wings.utils.WingsTestConstants.WORKFLOW_ID;
import static software.wings.utils.WingsTestConstants.WORKFLOW_NAME;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.ArtifactVariable;
import software.wings.beans.AzureConfig;
import software.wings.beans.EntityType;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.WebHookToken;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.JenkinsArtifactStream;
import software.wings.beans.trigger.ArtifactTriggerCondition;
import software.wings.beans.trigger.CustomPayloadExpression;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.GitHubPayloadSource;
import software.wings.beans.trigger.NewInstanceTriggerCondition;
import software.wings.beans.trigger.PipelineAction;
import software.wings.beans.trigger.PipelineTriggerCondition;
import software.wings.beans.trigger.ScheduledCondition;
import software.wings.beans.trigger.ScheduledTriggerCondition;
import software.wings.beans.trigger.ServiceInfraWorkflow;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.TriggerArtifactSelectionLastCollected;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.WebHookTriggerCondition;
import software.wings.beans.trigger.WebhookCondition;
import software.wings.beans.trigger.WebhookSource.GitHubEventType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerServiceTestHelper {
  public static Artifact artifact = anArtifact()
                                        .withAppId(APP_ID)
                                        .withUuid(ARTIFACT_ID)
                                        .withArtifactStreamId(ARTIFACT_STREAM_ID)
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

  public static DeploymentTrigger buildScheduledCondDeploymentTrigger() {
    return DeploymentTrigger.builder()
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(ScheduledCondition.builder().cronDescription("as").cronExpression("* * * * ?").build())
        .action(PipelineAction.builder()
                    .pipelineId(PIPELINE_ID)
                    .triggerArgs(TriggerArgs.builder()
                                     .triggerArtifactVariables(
                                         asList(TriggerArtifactVariable.builder()
                                                    .variableName(VARIABLE_NAME)
                                                    .variableValue(TriggerArtifactSelectionLastCollected.builder()
                                                                       .artifactStreamId(ARTIFACT_STREAM_ID)
                                                                       .artifactServerId(SETTING_ID)
                                                                       .build())
                                                    .entityId(ENTITY_ID)
                                                    .entityType(EntityType.SERVICE)
                                                    .build()))
                                     .build())
                    .build())
        .build();
  }

  public static DeploymentTrigger buildWebhookConditionTrigger() {
    return DeploymentTrigger.builder()
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .name(TRIGGER_NAME)
        .condition(WebhookCondition.builder()
                       .payloadSource(GitHubPayloadSource.builder()
                                          .gitHubEventTypes(asList(GitHubEventType.PUSH))
                                          .customPayloadExpressions(asList(
                                              CustomPayloadExpression.builder().expression("abc").value("def").build()))
                                          .build())
                       .webHookToken(WebHookToken.builder().build())
                       .build())
        .action(PipelineAction.builder()
                    .pipelineId(PIPELINE_ID)
                    .triggerArgs(TriggerArgs.builder()
                                     .triggerArtifactVariables(
                                         asList(TriggerArtifactVariable.builder()
                                                    .variableName(VARIABLE_NAME)
                                                    .variableValue(TriggerArtifactSelectionLastCollected.builder()
                                                                       .artifactStreamId(ARTIFACT_STREAM_ID)
                                                                       .artifactServerId(SETTING_ID)
                                                                       .build())
                                                    .entityId(ENTITY_ID)
                                                    .entityType(EntityType.SERVICE)
                                                    .build()))
                                     .build())
                    .build())
        .build();
  }

  public static Trigger buildWorkflowScheduledCondTrigger() {
    return Trigger.builder()
        .workflowId(WORKFLOW_ID)
        .uuid(TRIGGER_ID)
        .appId(APP_ID)
        .workflowType(ORCHESTRATION)
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
    Pipeline pipeline = Pipeline.builder()
                            .appId(APP_ID)
                            .uuid(PIPELINE_ID)
                            .services(asList(Service.builder().uuid(SERVICE_ID).name("Catalog").build()))
                            .build();
    List<Variable> userVariables = new ArrayList<>();
    userVariables.add(ArtifactVariable.builder()
                          .entityId(ENTITY_ID)
                          .name(VARIABLE_NAME)
                          .value(VARIABLE_VALUE)
                          .type(VariableType.ARTIFACT)
                          .build());

    pipeline.setPipelineVariables(userVariables);

    return pipeline;
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
    userVariables.add(aVariable().name("MyVar").value("MyVal").build());
    return aWorkflow()
        .envId(ENV_ID)
        .name(WORKFLOW_NAME)
        .appId(APP_ID)
        .serviceId(SERVICE_ID)
        .infraMappingId(INFRA_MAPPING_ID)
        .orchestrationWorkflow(aBasicOrchestrationWorkflow()
                                   .withUserVariables(userVariables)
                                   .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, "Pre-Deployment").build())
                                   .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT, "Post-Deployment").build())
                                   .build())
        .services(asList(Service.builder().uuid(SERVICE_ID).name("Catalog").build(),
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

  public static SettingAttribute buildSettingAttribute() {
    return aSettingAttribute()
        .withName(SETTING_NAME)
        .withUuid(SETTING_ID)
        .withValue(AzureConfig.builder().clientId("ClientId").tenantId("tenantId").key("key".toCharArray()).build())
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
}
