package software.wings.service.impl.yaml.handler.trigger;

import static io.harness.rule.OwnerRule.POOJA;
import static io.harness.rule.OwnerRule.PRABU;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.EntityType.INFRASTRUCTURE_DEFINITION;
import static software.wings.beans.Environment.Builder.anEnvironment;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.PageResponse;
import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Pipeline;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.trigger.Trigger;
import software.wings.beans.trigger.TriggerConditionType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.yaml.WorkflowYAMLHelper;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.tag.HarnessTagYamlHelper;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.yaml.handler.BaseYamlHandlerTest;
import software.wings.yaml.trigger.ArtifactTriggerConditionHandler;
import software.wings.yaml.trigger.PipelineTriggerConditionHandler;
import software.wings.yaml.trigger.ScheduledTriggerConditionHandler;
import software.wings.yaml.trigger.WebhookTriggerConditionHandler;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TriggerYamlHandlerTest extends BaseYamlHandlerTest {
  @Mock private YamlHelper mockYamlHelper;
  @Mock private YamlHandlerFactory mockYamlHandlerFactory;

  @Mock private AppService appService;
  @Mock private TriggerService triggerService;
  @Mock private HarnessTagYamlHelper harnessTagYamlHelper;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private EnvironmentService environmentService;
  @Mock private InfrastructureDefinitionService infrastructureDefinitionService;

  @Mock private SettingsService settingsService;

  @InjectMocks @Inject private TriggerYamlHandler handler;
  @InjectMocks @Inject private ArtifactTriggerConditionHandler artifactTriggerConditionHandler;
  @InjectMocks @Inject private PipelineTriggerConditionHandler pipelineTriggerConditionHandler;
  @InjectMocks @Inject private ScheduledTriggerConditionHandler scheduledTriggerConditionHandler;
  @InjectMocks @Inject private WorkflowYAMLHelper workflowYAMLHelper;

  @InjectMocks @Inject private WebhookTriggerConditionHandler webhookTriggerConditionHandler;
  @InjectMocks @Inject private ArtifactSelectionYamlHandler artifactSelectionYamlHandler;

  private final String yamlFilePath = "Setup/Applications/APP_NAME/Triggers/trigger.yaml";
  private final String resourcePath = "./triggers";

  @UtilityClass
  private static class validTriggerFiles {
    // On new artifact with Non templatised pipeline
    private static final String Trigger1 = "trigger1.yaml";
    // On pipeline completion with Env, Srv, Infra templatised pipeline
    private static final String Trigger2 = "trigger2.yaml";
    // On Schedule with Srv, Infra templatised pipeline
    private static final String Trigger3 = "trigger3.yaml";
    // On webhook with Env,Srv, infra templatised pipeline with variable values
    private static final String Trigger4 = "trigger4.yaml";
    // On git webhook with Env,Srv, infra templatised pipeline with concrete values
    private static final String Trigger5 = "trigger5.yaml";
    // On schedule with on new artifact only checked, with custom variables pipeline
    private static final String Trigger6 = "trigger6.yaml";
    // On GitLab with branch regex with verification variables, ssh credentials variables pipeline
    private static final String Trigger7 = "trigger7.yaml";

    // On new artifact with Non templatised workflow
    private static final String Trigger8 = "trigger8.yaml";
    // On pipeline completion with Env, Srv, Infra templatised pipeline
    private static final String Trigger9 = "trigger9.yaml";
    // On Schedule with Srv, Infra templatised pipeline
    private static final String Trigger10 = "trigger10.yaml";
    // On webhook with Env,Srv, infra templatised pipeline with variable values
    private static final String Trigger11 = "trigger11.yaml";
    // On git webhook with Env,Srv, infra templatised pipeline with concrete values
    private static final String Trigger12 = "trigger12.yaml";
    // On schedule with on new artifact only checked, with custom variables pipeline
    private static final String Trigger13 = "trigger13.yaml";
    // On GitLab with branch regex with verification variables, ssh credentials variables pipeline
    private static final String Trigger14 = "trigger14.yaml";

    private static final String TriggerPackage = "triggerPackage.yaml";
    private static final String TriggerPackageWrongAction = "triggerPackageWrongAction.yaml";

    private static final String TriggerRelease = "triggerRelease.yaml";

    private static final String TriggerReleaseWrongAction = "triggerReleaseWrongAction.yaml";
  }

  private ArgumentCaptor<Trigger> captor = ArgumentCaptor.forClass(Trigger.class);

  @Before
  public void setup() {
    doReturn(ACCOUNT_ID).when(appService).getAccountIdByAppId(anyString());
    doReturn(APP_ID).when(mockYamlHelper).getAppId(anyString(), anyString());
    doReturn("trigger").when(mockYamlHelper).extractEntityNameFromYamlPath(anyString(), anyString(), anyString());
    ArtifactStream artifactStream = DockerArtifactStream.builder().uuid("uuid").imageName("library-nginx-1").build();
    doReturn(artifactStream).when(mockYamlHelper).getArtifactStreamWithName(any(), any(), any());
    doReturn("k8s").when(mockYamlHelper).getServiceNameFromArtifactId(any(), any());
    doReturn("library_nginx").when(mockYamlHelper).getArtifactStreamName(any(), any());
    doReturn(Optional.of(anApplication().build()))
        .when(mockYamlHelper)
        .getApplicationIfPresent(anyString(), anyString());
    doReturn(handler).when(mockYamlHandlerFactory).getYamlHandler(YamlType.TRIGGER, null);
    doReturn(artifactTriggerConditionHandler)
        .when(mockYamlHandlerFactory)
        .getYamlHandler(YamlType.TRIGGER_CONDITION, "NEW_ARTIFACT");
    doReturn(webhookTriggerConditionHandler)
        .when(mockYamlHandlerFactory)
        .getYamlHandler(YamlType.TRIGGER_CONDITION, "WEBHOOK");
    doReturn(pipelineTriggerConditionHandler)
        .when(mockYamlHandlerFactory)
        .getYamlHandler(YamlType.TRIGGER_CONDITION, "PIPELINE_COMPLETION");
    doReturn(scheduledTriggerConditionHandler)
        .when(mockYamlHandlerFactory)
        .getYamlHandler(YamlType.TRIGGER_CONDITION, "SCHEDULED");
    doReturn(artifactSelectionYamlHandler).when(mockYamlHandlerFactory).getYamlHandler(YamlType.ARTIFACT_SELECTION);
    Service service = Service.builder().uuid("Service-id").name("k8s").build();
    doReturn(service).when(serviceResourceService).getServiceByName(anyString(), anyString());
    doReturn(service).when(serviceResourceService).getServiceByName(anyString(), anyString(), anyBoolean());
    doReturn(service).when(serviceResourceService).get(anyString(), anyString(), anyBoolean());

    Environment environment = anEnvironment().name("Prod").uuid("env-id").build();
    doReturn(environment).when(environmentService).getEnvironmentByName(anyString(), anyString(), anyBoolean());
    doReturn(environment).when(environmentService).get(anyString(), anyString(), anyBoolean());

    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder().name("Azure k8s").uuid("infra-id").build();
    doReturn(infrastructureDefinition)
        .when(infrastructureDefinitionService)
        .getInfraDefByName(anyString(), anyString(), anyString());
    doReturn(infrastructureDefinition).when(infrastructureDefinitionService).get(anyString(), anyString());

    SettingAttribute sshConnectionAttributes = aSettingAttribute().withUuid("ssh-id").withName("Wings Key").build();
    doReturn(sshConnectionAttributes).when(settingsService).fetchSettingAttributeByName(any(), any(), any());
    doReturn(sshConnectionAttributes).when(settingsService).get(anyString());
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger1() throws IOException {
    Pipeline pipeline = Pipeline.builder().uuid("pipeline-id").name("tp_1").build();
    doReturn(pipeline).when(mockYamlHelper).getPipelineFromName(any(), any());
    doReturn(pipeline).when(mockYamlHelper).getPipelineFromId(any(), any());
    testCRUD(validTriggerFiles.Trigger1, TriggerConditionType.NEW_ARTIFACT, WorkflowType.PIPELINE);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger2() throws IOException {
    Pipeline pipeline = Pipeline.builder().uuid("pipeline-id").name("tp_1").build();
    Variable variable1 = aVariable().name("srv").entityType(EntityType.SERVICE).build();
    Variable variable2 = aVariable().name("infra").entityType(INFRASTRUCTURE_DEFINITION).build();
    Map<String, Object> metadataEnv =
        ImmutableMap.of(Variable.RELATED_FIELD, "infra", Variable.ENTITY_TYPE, EntityType.ENVIRONMENT);
    Variable variable3 = aVariable().name("env").type(VariableType.ENTITY).metadata(metadataEnv).build();
    List<Variable> pipelineVariables = new ArrayList<>();
    pipelineVariables.add(variable1);
    pipelineVariables.add(variable2);
    pipelineVariables.add(variable3);
    pipeline.setPipelineVariables(pipelineVariables);

    doReturn(pipeline).when(mockYamlHelper).getPipelineFromName(any(), any());
    doReturn(pipeline).when(mockYamlHelper).getPipelineFromId(any(), any());
    testCRUD(validTriggerFiles.Trigger2, TriggerConditionType.PIPELINE_COMPLETION, WorkflowType.PIPELINE);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger3() throws IOException {
    Pipeline pipeline = Pipeline.builder().uuid("pipeline-id").name("tp_1").build();
    Variable variable1 = aVariable().name("srv").entityType(EntityType.SERVICE).build();
    Map<String, Object> metadataInfra =
        ImmutableMap.of(Variable.ENV_ID, "env-id", Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION.name());
    Variable variable2 = aVariable().name("infra").type(VariableType.ENTITY).metadata(metadataInfra).build();
    List<Variable> pipelineVariables = new ArrayList<>();
    pipelineVariables.add(variable1);
    pipelineVariables.add(variable2);
    pipeline.setPipelineVariables(pipelineVariables);

    doReturn(pipeline).when(mockYamlHelper).getPipelineFromName(any(), any());
    doReturn(pipeline).when(mockYamlHelper).getPipelineFromId(any(), any());
    testCRUD(validTriggerFiles.Trigger3, TriggerConditionType.SCHEDULED, WorkflowType.PIPELINE);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger4() throws IOException {
    Pipeline pipeline = Pipeline.builder().uuid("pipeline-id").name("tp_1").build();
    Variable variable1 = aVariable().name("srv").entityType(EntityType.SERVICE).build();
    Variable variable2 = aVariable().name("infra").entityType(INFRASTRUCTURE_DEFINITION).build();
    Map<String, Object> metadataEnv =
        ImmutableMap.of(Variable.RELATED_FIELD, "infra", Variable.ENTITY_TYPE, EntityType.ENVIRONMENT);
    Variable variable3 = aVariable().name("env").type(VariableType.ENTITY).metadata(metadataEnv).build();
    List<Variable> pipelineVariables = new ArrayList<>();
    pipelineVariables.add(variable1);
    pipelineVariables.add(variable2);
    pipelineVariables.add(variable3);
    pipeline.setPipelineVariables(pipelineVariables);

    doReturn(pipeline).when(mockYamlHelper).getPipelineFromName(any(), any());
    doReturn(pipeline).when(mockYamlHelper).getPipelineFromId(any(), any());
    testCRUD(validTriggerFiles.Trigger4, TriggerConditionType.WEBHOOK, WorkflowType.PIPELINE);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger5() throws IOException {
    Pipeline pipeline = Pipeline.builder().uuid("pipeline-id").name("tp_1").build();
    Variable variable1 = aVariable().name("srv").entityType(EntityType.SERVICE).build();
    Variable variable2 = aVariable().name("infra").entityType(INFRASTRUCTURE_DEFINITION).build();
    Map<String, Object> metadataEnv =
        ImmutableMap.of(Variable.RELATED_FIELD, "infra", Variable.ENTITY_TYPE, EntityType.ENVIRONMENT);
    Variable variable3 = aVariable().name("env").type(VariableType.ENTITY).metadata(metadataEnv).build();
    List<Variable> pipelineVariables = new ArrayList<>();
    pipelineVariables.add(variable1);
    pipelineVariables.add(variable2);
    pipelineVariables.add(variable3);
    pipeline.setPipelineVariables(pipelineVariables);

    doReturn(pipeline).when(mockYamlHelper).getPipelineFromName(any(), any());
    doReturn(pipeline).when(mockYamlHelper).getPipelineFromId(any(), any());
    testCRUD(validTriggerFiles.Trigger5, TriggerConditionType.WEBHOOK, WorkflowType.PIPELINE);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger6() throws IOException {
    Pipeline pipeline = Pipeline.builder().uuid("pipeline-id").name("tp_1").build();
    Variable variable1 = aVariable().name("var2").type(VariableType.TEXT).build();
    Variable variable2 = aVariable().name("test-pip").type(VariableType.TEXT).build();
    List<Variable> pipelineVariables = new ArrayList<>();
    pipelineVariables.add(variable1);
    pipelineVariables.add(variable2);
    pipeline.setPipelineVariables(pipelineVariables);

    doReturn(pipeline).when(mockYamlHelper).getPipelineFromName(any(), any());
    doReturn(pipeline).when(mockYamlHelper).getPipelineFromId(any(), any());
    testCRUD(validTriggerFiles.Trigger6, TriggerConditionType.SCHEDULED, WorkflowType.PIPELINE);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger7() throws IOException {
    Variable variable1 = aVariable().name("app_app").entityType(EntityType.APPDYNAMICS_APPID).build();
    Variable variable2 = aVariable().name("Napp").entityType(EntityType.NEWRELIC_APPID).build();
    Variable variable3 = aVariable().name("Nserver").entityType(EntityType.NEWRELIC_CONFIGID).build();
    Variable variable4 = aVariable().name("ssh").entityType(EntityType.SS_SSH_CONNECTION_ATTRIBUTE).build();
    Variable variable5 = aVariable().name("app_server").entityType(EntityType.APPDYNAMICS_CONFIGID).build();
    Variable variable6 = aVariable().name("app_tier").entityType(EntityType.APPDYNAMICS_TIERID).build();

    List<Variable> pipelineVariables = new ArrayList<>();
    pipelineVariables.add(variable1);
    pipelineVariables.add(variable2);
    pipelineVariables.add(variable3);
    pipelineVariables.add(variable4);
    pipelineVariables.add(variable5);
    pipelineVariables.add(variable6);

    Pipeline pipeline = Pipeline.builder().uuid("pipeline-id").name("tp_1").build();
    pipeline.setPipelineVariables(pipelineVariables);
    doReturn(pipeline).when(mockYamlHelper).getPipelineFromName(any(), any());
    doReturn(pipeline).when(mockYamlHelper).getPipelineFromId(any(), any());
    testCRUD(validTriggerFiles.Trigger7, TriggerConditionType.WEBHOOK, WorkflowType.PIPELINE);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger8() throws IOException {
    Workflow workflow = aWorkflow().uuid("workflow-id").name("w1").build();
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromId(any(), any());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromName(any(), any());
    testCRUD(validTriggerFiles.Trigger8, TriggerConditionType.NEW_ARTIFACT, WorkflowType.ORCHESTRATION);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger9() throws IOException {
    Workflow workflow = aWorkflow().uuid("workflow-id").name("w1").build();
    Variable variable1 = aVariable().name("Service").entityType(EntityType.SERVICE).build();
    Variable variable2 = aVariable().name("InfraDefinition_KUBERNETES").entityType(INFRASTRUCTURE_DEFINITION).build();
    Map<String, Object> metadataEnv = ImmutableMap.of(
        Variable.RELATED_FIELD, "InfraDefinition_KUBERNETES", Variable.ENTITY_TYPE, EntityType.ENVIRONMENT);
    Variable variable3 = aVariable().name("Environment").type(VariableType.ENTITY).metadata(metadataEnv).build();
    List<Variable> userVariables = new ArrayList<>();
    userVariables.add(variable1);
    userVariables.add(variable2);
    userVariables.add(variable3);
    workflow.setOrchestrationWorkflow(aCanaryOrchestrationWorkflow().withUserVariables(userVariables).build());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromId(any(), any());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromName(any(), any());

    Pipeline pipeline = Pipeline.builder().uuid("pipeline-id").name("tp_1").build();
    doReturn(pipeline).when(mockYamlHelper).getPipelineFromName(any(), any());
    testCRUD(validTriggerFiles.Trigger9, TriggerConditionType.PIPELINE_COMPLETION, WorkflowType.ORCHESTRATION);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger10() throws IOException {
    Workflow workflow = aWorkflow().uuid("workflow-id").name("w1").envId("env-id").build();
    Variable variable1 = aVariable().name("Service").entityType(EntityType.SERVICE).build();
    Map<String, Object> metadataInfra =
        ImmutableMap.of(Variable.ENV_ID, "env-id", Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION.name());
    Variable variable2 =
        aVariable().name("InfraDefinition_Kubernetes").type(VariableType.ENTITY).metadata(metadataInfra).build();
    List<Variable> userVariables = new ArrayList<>();
    userVariables.add(variable1);
    userVariables.add(variable2);
    workflow.setOrchestrationWorkflow(aCanaryOrchestrationWorkflow().withUserVariables(userVariables).build());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromId(any(), any());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromName(any(), any());
    testCRUD(validTriggerFiles.Trigger10, TriggerConditionType.SCHEDULED, WorkflowType.ORCHESTRATION);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger11() throws IOException {
    Workflow workflow = aWorkflow().uuid("workflow-id").name("w1").envId("env-id").build();
    Variable variable1 = aVariable().name("Service").entityType(EntityType.SERVICE).build();
    Variable variable2 = aVariable().name("InfraDefinition_KUBERNETES").entityType(INFRASTRUCTURE_DEFINITION).build();
    Map<String, Object> metadataEnv = ImmutableMap.of(
        Variable.RELATED_FIELD, "InfraDefinition_KUBERNETES", Variable.ENTITY_TYPE, EntityType.ENVIRONMENT);
    Variable variable3 = aVariable().name("Environment").type(VariableType.ENTITY).metadata(metadataEnv).build();
    List<Variable> userVariables = new ArrayList<>();
    userVariables.add(variable1);
    userVariables.add(variable2);
    userVariables.add(variable3);
    workflow.setOrchestrationWorkflow(aCanaryOrchestrationWorkflow().withUserVariables(userVariables).build());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromId(any(), any());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromName(any(), any());
    testCRUD(validTriggerFiles.Trigger11, TriggerConditionType.WEBHOOK, WorkflowType.ORCHESTRATION);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger12() throws IOException {
    Workflow workflow = aWorkflow().uuid("workflow-id").name("w1").envId("env-id").build();
    Variable variable1 = aVariable().name("Service").entityType(EntityType.SERVICE).build();
    Variable variable2 = aVariable().name("InfraDefinition_KUBERNETES").entityType(INFRASTRUCTURE_DEFINITION).build();
    Map<String, Object> metadataEnv = ImmutableMap.of(
        Variable.RELATED_FIELD, "InfraDefinition_KUBERNETES", Variable.ENTITY_TYPE, EntityType.ENVIRONMENT);
    Variable variable3 = aVariable().name("Environment").type(VariableType.ENTITY).metadata(metadataEnv).build();
    List<Variable> userVariables = new ArrayList<>();
    userVariables.add(variable1);
    userVariables.add(variable2);
    userVariables.add(variable3);
    workflow.setOrchestrationWorkflow(aCanaryOrchestrationWorkflow().withUserVariables(userVariables).build());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromId(any(), any());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromName(any(), any());
    testCRUD(validTriggerFiles.Trigger12, TriggerConditionType.WEBHOOK, WorkflowType.ORCHESTRATION);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger13() throws IOException {
    Workflow workflow = aWorkflow().uuid("workflow-id").name("w1").envId("env-id").build();
    Variable variable1 = aVariable().name("var4").type(VariableType.TEXT).build();
    Variable variable2 = aVariable().name("var3").type(VariableType.TEXT).build();
    Variable variable3 = aVariable().name("var1").type(VariableType.TEXT).build();

    List<Variable> userVariables = new ArrayList<>();
    userVariables.add(variable1);
    userVariables.add(variable2);
    userVariables.add(variable3);

    workflow.setOrchestrationWorkflow(aCanaryOrchestrationWorkflow().withUserVariables(userVariables).build());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromId(any(), any());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromName(any(), any());
    testCRUD(validTriggerFiles.Trigger13, TriggerConditionType.SCHEDULED, WorkflowType.ORCHESTRATION);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testCrudTrigger14() throws IOException {
    Workflow workflow = aWorkflow().uuid("workflow-id").name("w1").envId("env-id").build();
    Variable variable1 = aVariable().name("AppDynamics_Application").entityType(EntityType.APPDYNAMICS_APPID).build();
    Variable variable2 = aVariable().name("NewRelic_Application").entityType(EntityType.NEWRELIC_APPID).build();
    Variable variable3 = aVariable().name("NewRelic_Server").entityType(EntityType.NEWRELIC_CONFIGID).build();
    Variable variable4 =
        aVariable().name("SSH_ConnectionAttribute").entityType(EntityType.SS_SSH_CONNECTION_ATTRIBUTE).build();
    Variable variable5 = aVariable().name("AppDynamics_Server").entityType(EntityType.APPDYNAMICS_CONFIGID).build();
    Variable variable6 = aVariable().name("AppDynamics_Tier").entityType(EntityType.APPDYNAMICS_TIERID).build();

    List<Variable> userVariables = new ArrayList<>();
    userVariables.add(variable1);
    userVariables.add(variable2);
    userVariables.add(variable3);
    userVariables.add(variable4);
    userVariables.add(variable5);
    userVariables.add(variable6);

    workflow.setOrchestrationWorkflow(aCanaryOrchestrationWorkflow().withUserVariables(userVariables).build());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromId(any(), any());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromName(any(), any());
    testCRUD(validTriggerFiles.Trigger14, TriggerConditionType.WEBHOOK, WorkflowType.ORCHESTRATION);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testCrudTriggerPackage() throws IOException {
    Workflow workflow = aWorkflow().uuid("workflow-id").name("w1").envId("env-id").build();
    workflow.setOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromId(any(), any());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromName(any(), any());
    testCRUD(validTriggerFiles.TriggerPackage, TriggerConditionType.WEBHOOK, WorkflowType.ORCHESTRATION);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testCrudTriggerPackageForWrongAction() {
    Workflow workflow = aWorkflow().uuid("workflow-id").name("w1").envId("env-id").build();
    workflow.setOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromId(any(), any());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromName(any(), any());
    assertThatThrownBy(()
                           -> testCRUD(validTriggerFiles.TriggerPackageWrongAction, TriggerConditionType.WEBHOOK,
                               WorkflowType.ORCHESTRATION))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Unsupported Github action package:opened.");
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testCrudTriggerRelease() throws IOException {
    Workflow workflow = aWorkflow().uuid("workflow-id").name("w1").envId("env-id").build();
    workflow.setOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromId(any(), any());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromName(any(), any());
    testCRUD(validTriggerFiles.TriggerRelease, TriggerConditionType.WEBHOOK, WorkflowType.ORCHESTRATION);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void testCrudTriggerReleaseForWrongAction() {
    Workflow workflow = aWorkflow().uuid("workflow-id").name("w1").envId("env-id").build();
    workflow.setOrchestrationWorkflow(aCanaryOrchestrationWorkflow().build());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromId(any(), any());
    doReturn(workflow).when(mockYamlHelper).getWorkflowFromName(any(), any());
    assertThatThrownBy(()
                           -> testCRUD(validTriggerFiles.TriggerReleaseWrongAction, TriggerConditionType.WEBHOOK,
                               WorkflowType.ORCHESTRATION))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Unsupported Release action opened.");
  }

  private void testCRUD(String yamlFileName, TriggerConditionType conditionType, WorkflowType actionType)
      throws IOException {
    doReturn(null).when(mockYamlHelper).getTrigger(anyString(), anyString());
    File yamlFile = null;
    try {
      yamlFile =
          new File(getClass().getClassLoader().getResource(resourcePath + PATH_DELIMITER + yamlFileName).toURI());
    } catch (URISyntaxException e) {
      Assertions.fail("Unable to find yaml file " + yamlFileName);
    }
    assertThat(yamlFile).isNotNull();
    String yamlString = FileUtils.readFileToString(yamlFile, "UTF-8");
    ChangeContext<Trigger.Yaml> changeContext = getChangeContext(yamlString);
    Trigger.Yaml yaml = (Trigger.Yaml) getYaml(yamlString, Trigger.Yaml.class);
    changeContext.setYaml(yaml);

    handler.upsertFromYaml(changeContext, Arrays.asList(changeContext));
    verify(triggerService).save(captor.capture());
    Trigger savedTrigger = captor.getValue();
    doReturn(savedTrigger).when(mockYamlHelper).getTrigger(anyString(), anyString());

    handler.upsertFromYaml(changeContext, Arrays.asList(changeContext));

    assertThat(savedTrigger).isNotNull();
    assertThat(conditionType).isEqualTo(savedTrigger.getCondition().getConditionType());
    assertThat(actionType).isEqualTo(savedTrigger.getWorkflowType());

    yaml = handler.toYaml(savedTrigger, APP_ID);

    assertThat(yaml).isNotNull();
    String yamlContent = getYamlContent(yaml);
    assertThat(yamlContent).isNotNull();
    yamlContent = yamlContent.substring(0, yamlContent.length() - 1);
    assertThat(yamlContent).isEqualTo(yamlString);

    PageResponse<Trigger> pageResponse = new PageResponse<>();
    List<Trigger> triggerList = new ArrayList<>();
    triggerList.add(savedTrigger);
    pageResponse.setResponse(triggerList);
    doReturn(pageResponse).when(triggerService).list(any(), anyBoolean(), anyString());
    Trigger retrievedTrigger = handler.get(ACCOUNT_ID, yamlFilePath);

    assertThat(retrievedTrigger).isNotNull();
    assertThat(retrievedTrigger.getUuid()).isEqualTo(savedTrigger.getUuid());

    doReturn(savedTrigger).when(mockYamlHelper).getTrigger(anyString(), anyString());

    handler.delete(changeContext);

    verify(triggerService).delete(anyString(), anyString());
    reset(triggerService);
  }

  private ChangeContext<Trigger.Yaml> getChangeContext(String validYamlContent) {
    GitFileChange gitFileChange = GitFileChange.Builder.aGitFileChange()
                                      .withAccountId(ACCOUNT_ID)
                                      .withFilePath(yamlFilePath)
                                      .withFileContent(validYamlContent)
                                      .build();

    ChangeContext<Trigger.Yaml> changeContext = new ChangeContext<>();
    changeContext.setChange(gitFileChange);
    changeContext.setYamlType(YamlType.TRIGGER);
    changeContext.setYamlSyncHandler(handler);
    return changeContext;
  }
}