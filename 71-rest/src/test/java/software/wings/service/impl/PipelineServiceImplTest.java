package software.wings.service.impl;

import static io.harness.rule.OwnerRule.ABHINAV;
import static io.harness.rule.OwnerRule.POOJA;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.EntityType.APPDYNAMICS_APPID;
import static software.wings.beans.EntityType.APPDYNAMICS_CONFIGID;
import static software.wings.beans.EntityType.APPDYNAMICS_TIERID;
import static software.wings.beans.EntityType.ELK_CONFIGID;
import static software.wings.beans.EntityType.ELK_INDICES;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_DEFINITION;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.NEWRELIC_APPID;
import static software.wings.beans.EntityType.NEWRELIC_CONFIGID;
import static software.wings.beans.EntityType.NEWRELIC_MARKER_APPID;
import static software.wings.beans.EntityType.NEWRELIC_MARKER_CONFIGID;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.SPLUNK_CONFIGID;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.mockChecker;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.limits.LimitCheckerFactory;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mongodb.morphia.query.Query;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.Workflow;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlPushService;
import software.wings.sm.StateMachine;
import software.wings.sm.StateType;
import software.wings.sm.states.ApprovalState.ApprovalStateKeys;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PipelineServiceImpl.class)
@PowerMockIgnore({"javax.net.*"})
public class PipelineServiceImplTest extends WingsBaseTest {
  @Mock AppService mockAppService;
  @Mock WingsPersistence mockWingsPersistence;
  @Mock YamlPushService mockYamlPushService;
  @Mock WorkflowService mockWorkflowService;
  @Mock private LimitCheckerFactory mockLimitCheckerFactory;
  @Inject @InjectMocks private PipelineServiceImpl pipelineServiceImpl;

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testPopulateParentFields() {
    List<Variable> workflowVariables = asList(
        aVariable()
            .entityType(SERVICE)
            .name("Service")
            .value("Service 1")
            .metadata(ImmutableMap.of(Variable.RELATED_FIELD, "ServiceInfra_ECS", Variable.ENTITY_TYPE, SERVICE))
            .build(),
        aVariable()
            .entityType(ENVIRONMENT)
            .name("Environment")
            .value("env 1")
            .metadata(ImmutableMap.of(Variable.RELATED_FIELD, "ServiceInfra_ECS", Variable.ENTITY_TYPE, ENVIRONMENT))
            .build(),
        aVariable().entityType(INFRASTRUCTURE_MAPPING).name("ServiceInfra_ECS").value("${Infra}").build(),
        aVariable()
            .entityType(APPDYNAMICS_APPID)
            .name("AppdAppId")
            .value("AppD app")
            .metadata(ImmutableMap.of(Variable.RELATED_FIELD, "AppdTierId", Variable.ENTITY_TYPE, APPDYNAMICS_APPID))
            .build(),
        aVariable()
            .entityType(APPDYNAMICS_CONFIGID)
            .name("AppdConfigId")
            .value("AppD config")
            .metadata(ImmutableMap.of(Variable.RELATED_FIELD, "AppdTierId", Variable.ENTITY_TYPE, APPDYNAMICS_CONFIGID))
            .build(),
        aVariable()
            .entityType(ELK_CONFIGID)
            .name("ElkConfigId")
            .value("elk config")
            .metadata(ImmutableMap.of(Variable.RELATED_FIELD, "ElkIndices", Variable.ENTITY_TYPE, ELK_CONFIGID))
            .build(),
        aVariable().entityType(ELK_INDICES).name("ElkIndices").value("elk indices").build(),
        aVariable().entityType(NEWRELIC_APPID).name("NewRelicAppId").value("NewRelic appId").build(),
        aVariable()
            .entityType(NEWRELIC_CONFIGID)
            .name("NewRelicConfigId")
            .value("NewRelic configId")
            .metadata(ImmutableMap.of(Variable.RELATED_FIELD, "NewRelicAppId", Variable.ENTITY_TYPE, NEWRELIC_CONFIGID))
            .build(),
        aVariable().entityType(SPLUNK_CONFIGID).name("SplunkConfigId").value("Splunk configId").build(),
        aVariable()
            .entityType(NEWRELIC_MARKER_APPID)
            .name("NewRelicMarkerAppId")
            .value("NewRelic Marker appId")
            .build(),
        aVariable()
            .entityType(NEWRELIC_MARKER_CONFIGID)
            .name("NewRelicMarkerConfigId")
            .value("NewRelic Marker configId")
            .metadata(ImmutableMap.of(
                Variable.RELATED_FIELD, "NewRelicMarkerAppId", Variable.ENTITY_TYPE, NEWRELIC_MARKER_CONFIGID))
            .build(),
        aVariable().entityType(APPDYNAMICS_TIERID).name("AppdTierId").value("${Tier}").build(),
        aVariable().type(VariableType.TEXT).name("test").value("test").build());

    Map<String, String> pseWorkflowVariables = new HashMap<>();
    pseWorkflowVariables.put("Service", "Service 2");
    pseWorkflowVariables.put("Environment", "Environment 2");
    pseWorkflowVariables.put("ServiceInfra_ECS", "{$infra}");
    pseWorkflowVariables.put("AppdAppId", "AppD app 1");
    pseWorkflowVariables.put("AppdTierId", "AppD tier");
    pseWorkflowVariables.put("AppdConfigId", "AppD config 2");
    pseWorkflowVariables.put("NewRelicAppId", "NewRelicAppId2");
    pseWorkflowVariables.put("NewRelicMarkerAppId", "${app}");
    pseWorkflowVariables.put("ElkIndices", "elk");
    pseWorkflowVariables.put("ElkConfigId", "elkconfig");
    pseWorkflowVariables.put("NewRelicMarkerConfigId", "newRelicMarkerconfigId");
    pseWorkflowVariables.put("NewRelicConfigId", "newRelicConfigId");

    // Infra mapping variable populate test
    Map<String, Object> metadataMapInfra = new HashMap<>();
    metadataMapInfra.put("entityType", INFRASTRUCTURE_MAPPING);
    Variable infraMappingPipelineVar = aVariable()
                                           .name("Infra")
                                           .description("Variable for Service Infra-structure entity")
                                           .type(VariableType.ENTITY)
                                           .mandatory(true)
                                           .fixed(false)
                                           .metadata(metadataMapInfra)
                                           .build();
    pipelineServiceImpl.populateParentFields(infraMappingPipelineVar, INFRASTRUCTURE_MAPPING, workflowVariables,
        "ServiceInfra_ECS", pseWorkflowVariables, false);
    assertThat(infraMappingPipelineVar.getMetadata().get(Variable.ENV_ID)).isNotNull();
    assertThat(infraMappingPipelineVar.getMetadata().get(Variable.ENV_ID)).isEqualTo("Environment 2");

    assertThat(infraMappingPipelineVar.getMetadata().get(Variable.SERVICE_ID)).isNotNull();
    assertThat(infraMappingPipelineVar.getMetadata().get(Variable.SERVICE_ID)).isEqualTo("Service 2");

    // Infra mapping definition populate test
    Map<String, Object> metadataMapInfraDef = new HashMap<>();
    metadataMapInfra.put("entityType", INFRASTRUCTURE_DEFINITION);
    Variable infraDefPipelineVar = aVariable()
                                       .name("Infra")
                                       .description("Variable for Service Infra-structure entity")
                                       .type(VariableType.ENTITY)
                                       .mandatory(true)
                                       .fixed(false)
                                       .metadata(metadataMapInfraDef)
                                       .build();

    pipelineServiceImpl.populateParentFields(infraDefPipelineVar, INFRASTRUCTURE_DEFINITION, workflowVariables,
        "ServiceInfra_ECS", pseWorkflowVariables, false);
    assertThat(infraDefPipelineVar.getMetadata().get(Variable.ENV_ID)).isNotNull();
    assertThat(infraDefPipelineVar.getMetadata().get(Variable.ENV_ID)).isEqualTo("Environment 2");

    assertThat(infraDefPipelineVar.getMetadata().get(Variable.SERVICE_ID)).isNotNull();
    assertThat(infraDefPipelineVar.getMetadata().get(Variable.SERVICE_ID)).isEqualTo("Service 2");

    // Appdynamics tierId populate test

    Map<String, Object> metadataMapAppdTier = new HashMap<>();
    metadataMapAppdTier.put("entityType", APPDYNAMICS_TIERID);
    Variable appdTierPipelineVar = aVariable()
                                       .name("Tier")
                                       .description("Variable for Appd Tier")
                                       .type(VariableType.ENTITY)
                                       .mandatory(true)
                                       .fixed(false)
                                       .metadata(metadataMapAppdTier)
                                       .build();

    pipelineServiceImpl.populateParentFields(
        appdTierPipelineVar, APPDYNAMICS_TIERID, workflowVariables, "AppdTierId", pseWorkflowVariables, false);
    assertThat(appdTierPipelineVar.getMetadata().get(Variable.PARENT_FIELDS)).isNotNull();
    Map<String, String> parents = (Map<String, String>) appdTierPipelineVar.getMetadata().get(Variable.PARENT_FIELDS);
    assertThat(parents.get("applicationId")).isEqualTo("AppD app 1");
    assertThat(parents.get("analysisServerConfigId")).isEqualTo("AppD config 2");

    // Appdynamics appId populate test
    Map<String, Object> metadataMapAppdApp = new HashMap<>();
    metadataMapAppdApp.put("entityType", APPDYNAMICS_APPID);
    Variable appdAppPipelineVar = aVariable()
                                      .name("App")
                                      .description("Variable for Appd App")
                                      .type(VariableType.ENTITY)
                                      .mandatory(true)
                                      .fixed(false)
                                      .metadata(metadataMapAppdApp)
                                      .build();

    pipelineServiceImpl.populateParentFields(
        appdAppPipelineVar, APPDYNAMICS_APPID, workflowVariables, "AppdTierId", pseWorkflowVariables, false);
    assertThat(appdAppPipelineVar.getMetadata().get(Variable.PARENT_FIELDS)).isNotNull();
    parents = (Map<String, String>) appdAppPipelineVar.getMetadata().get(Variable.PARENT_FIELDS);
    assertThat(parents.get("analysisServerConfigId")).isEqualTo("AppD config 2");

    // Elk indices populate test
    Map<String, Object> metadataMapElkIndices = new HashMap<>();
    metadataMapElkIndices.put("entityType", ELK_INDICES);
    Variable elkIndicesPipelineVar = aVariable()
                                         .name("Elk")
                                         .description("Variable for elk type")
                                         .type(VariableType.ENTITY)
                                         .mandatory(true)
                                         .fixed(false)
                                         .metadata(metadataMapElkIndices)
                                         .build();

    pipelineServiceImpl.populateParentFields(
        elkIndicesPipelineVar, ELK_INDICES, workflowVariables, "ElkIndices", pseWorkflowVariables, false);
    assertThat(elkIndicesPipelineVar.getMetadata().get(Variable.PARENT_FIELDS)).isNotNull();
    parents = (Map<String, String>) elkIndicesPipelineVar.getMetadata().get(Variable.PARENT_FIELDS);
    assertThat(parents.get("analysisServerConfigId")).isEqualTo("elkconfig");

    // New relic populate test
    Map<String, Object> metadataMapNewRelicIndices = new HashMap<>();
    metadataMapNewRelicIndices.put("entityType", NEWRELIC_APPID);
    Variable newRelicPipelineVar = aVariable()
                                       .name("newRelic")
                                       .description("Variable for newrelic type")
                                       .type(VariableType.ENTITY)
                                       .mandatory(true)
                                       .fixed(false)
                                       .metadata(metadataMapNewRelicIndices)
                                       .build();

    pipelineServiceImpl.populateParentFields(
        newRelicPipelineVar, NEWRELIC_APPID, workflowVariables, "NewRelicAppId", pseWorkflowVariables, false);
    assertThat(newRelicPipelineVar.getMetadata().get(Variable.PARENT_FIELDS)).isNotNull();
    parents = (Map<String, String>) newRelicPipelineVar.getMetadata().get(Variable.PARENT_FIELDS);
    assertThat(parents.get("analysisServerConfigId")).isEqualTo("newRelicConfigId");

    // New relic marker populate test
    Map<String, Object> metadataMapNewRelicMarkerIndices = new HashMap<>();
    metadataMapNewRelicMarkerIndices.put("entityType", NEWRELIC_MARKER_APPID);
    Variable newRelicMarkerPipelineVar = aVariable()
                                             .name("newRelic")
                                             .description("Variable for newrelic type")
                                             .type(VariableType.ENTITY)
                                             .mandatory(true)
                                             .fixed(false)
                                             .metadata(metadataMapNewRelicMarkerIndices)
                                             .build();

    pipelineServiceImpl.populateParentFields(newRelicMarkerPipelineVar, NEWRELIC_MARKER_APPID, workflowVariables,
        "NewRelicMarkerAppId", pseWorkflowVariables, false);
    assertThat(newRelicMarkerPipelineVar.getMetadata().get(Variable.PARENT_FIELDS)).isNotNull();
    parents = (Map<String, String>) newRelicMarkerPipelineVar.getMetadata().get(Variable.PARENT_FIELDS);
    assertThat(parents.get("analysisServerConfigId")).isEqualTo("newRelicMarkerconfigId");

    pipelineServiceImpl.populateParentFields(
        newRelicMarkerPipelineVar, NEWRELIC_MARKER_APPID, null, "NewRelicMarkerAppId", pseWorkflowVariables, false);
    assertThat(newRelicMarkerPipelineVar.getMetadata().get(Variable.PARENT_FIELDS)).isNotNull();
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testUpdateRelatedFieldsEnvironmentInfraMapping() throws Exception {
    List<Variable> workflowVariables = asList(aVariable().entityType(SERVICE).name("Service").build(),
        aVariable().entityType(ENVIRONMENT).name("Environment").build(),
        aVariable().entityType(INFRASTRUCTURE_MAPPING).name("ServiceInfra_ECS").value("${infra}").build());

    Map<String, Object> metadata = new HashMap<>();
    metadata.put(Variable.RELATED_FIELD, "infra");
    metadata.put(Variable.ENTITY_TYPE, ENVIRONMENT);
    Variable pipelineVariable = aVariable().entityType(ENVIRONMENT).name("Environment").metadata(metadata).build();
    Map<String, String> pseWorkflowVariables = new HashMap<>();
    pseWorkflowVariables.put("Service", "Service 2");
    pseWorkflowVariables.put("Environment", "Environment");
    pseWorkflowVariables.put("ServiceInfra_ECS", "${infra2}");

    pipelineServiceImpl.updateRelatedFieldEnvironment(false, workflowVariables, pseWorkflowVariables, pipelineVariable);
    assertThat(pipelineVariable.getMetadata().get("relatedField")).isEqualTo("infra,infra2");
  }

  @Test
  @Owner(developers = ABHINAV)
  @Category(UnitTests.class)
  public void testUniquePipelineName() throws Exception {
    PipelineStageElement pipelineStageElement = PipelineStageElement.builder().name("test").build();
    PipelineStageElement pipelineStageElement1 = PipelineStageElement.builder().name("test_1").build();

    PipelineStage pipelineStage =
        PipelineStage.builder().pipelineStageElements(Arrays.asList(pipelineStageElement)).build();
    Pipeline pipeline = Pipeline.builder().pipelineStages(Arrays.asList(pipelineStage, pipelineStage)).build();

    assertThatThrownBy(() -> pipelineServiceImpl.checkUniquePipelineStepName(pipeline))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Duplicate step name test");

    PipelineStage pipelineStage1 =
        PipelineStage.builder().pipelineStageElements(Arrays.asList(pipelineStageElement1)).build();
    Pipeline pipeline1 = Pipeline.builder().pipelineStages(Arrays.asList(pipelineStage1, pipelineStage)).build();

    pipelineServiceImpl.checkUniquePipelineStepName(pipeline1);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testHandlePipelineStageDeletion() throws Exception {
    PipelineStageElement pipelineStageElement = PipelineStageElement.builder().name("test").parallelIndex(0).build();
    PipelineStageElement pipelineStageElement1 = PipelineStageElement.builder().name("test_1").parallelIndex(0).build();
    PipelineStageElement pipelineStageElement2 = PipelineStageElement.builder().name("test_2").parallelIndex(1).build();
    PipelineStageElement pipelineStageElement3 = PipelineStageElement.builder().name("test_3").parallelIndex(1).build();
    PipelineStageElement pipelineStageElement4 = PipelineStageElement.builder().name("test_4").parallelIndex(1).build();

    PipelineStage pipelineStage =
        PipelineStage.builder().pipelineStageElements(Arrays.asList(pipelineStageElement)).parallel(false).build();
    PipelineStage pipelineStage1 =
        PipelineStage.builder().pipelineStageElements(Arrays.asList(pipelineStageElement1)).parallel(true).build();
    PipelineStage pipelineStage2 =
        PipelineStage.builder().pipelineStageElements(Arrays.asList(pipelineStageElement2)).parallel(false).build();
    PipelineStage pipelineStage3 =
        PipelineStage.builder().pipelineStageElements(Arrays.asList(pipelineStageElement3)).parallel(true).build();
    PipelineStage pipelineStage4 =
        PipelineStage.builder().pipelineStageElements(Arrays.asList(pipelineStageElement4)).parallel(true).build();

    Pipeline pipeline =
        Pipeline.builder()
            .pipelineStages(Arrays.asList(pipelineStage, pipelineStage1, pipelineStage3, pipelineStage4))
            .build();
    Pipeline savedPipeline = Pipeline.builder()
                                 .pipelineStages(Arrays.asList(
                                     pipelineStage, pipelineStage1, pipelineStage2, pipelineStage3, pipelineStage4))
                                 .build();

    pipelineServiceImpl.handlePipelineStageDeletion(pipeline, savedPipeline, false);

    assertThat(pipeline.getPipelineStages().get(2).isParallel()).isEqualTo(false);
    assertThat(pipeline.getPipelineStages().get(2).getPipelineStageElements().get(0).getName()).isEqualTo("test_3");

    pipeline = Pipeline.builder()
                   .pipelineStages(Arrays.asList(pipelineStage, pipelineStage1, pipelineStage2, pipelineStage4))
                   .build();
    savedPipeline = Pipeline.builder()
                        .pipelineStages(Arrays.asList(
                            pipelineStage, pipelineStage1, pipelineStage2, pipelineStage3, pipelineStage4))
                        .build();

    pipelineServiceImpl.handlePipelineStageDeletion(pipeline, savedPipeline, false);

    assertThat(pipeline.getPipelineStages().get(3).isParallel()).isEqualTo(true);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldNotAllowSamePublishedVariable() throws Exception {
    Map<String, Object> approvalProperties = new HashMap<>();
    approvalProperties.put(ApprovalStateKeys.sweepingOutputName, "Info");
    Pipeline pipeline =
        Pipeline.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .name("test")
            .pipelineStages(asList(PipelineStage.builder()
                                       .pipelineStageElements(asList(PipelineStageElement.builder()
                                                                         .name("approval_state_1")
                                                                         .type(StateType.APPROVAL.name())
                                                                         .properties(approvalProperties)
                                                                         .build()))
                                       .build(),
                PipelineStage.builder()
                    .pipelineStageElements(asList(PipelineStageElement.builder()
                                                      .name("approval_state_2")
                                                      .type(StateType.APPROVAL.name())
                                                      .properties(approvalProperties)
                                                      .build()))
                    .build()))
            .build();

    setupQueryMocks();

    assertThatThrownBy(() -> pipelineServiceImpl.save(pipeline))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("You cannot use the same Publish Variable Name");
  }

  private void setupQueryMocks() throws Exception {
    PowerMockito.doReturn(ACCOUNT_ID).when(mockAppService).getAccountIdByAppId(APP_ID);
    Query mockQuery = PowerMockito.mock(Query.class);
    PowerMockito.doReturn(mockChecker()).when(mockLimitCheckerFactory).getInstance(Mockito.any());
    PowerMockito.doReturn(null).when(mockWingsPersistence).save(any(Pipeline.class));
    PowerMockito.doReturn(mockQuery).when(mockWingsPersistence).createQuery(Pipeline.class);
    PowerMockito.doReturn(mockQuery).when(mockQuery).filter(anyString(), any());
    PowerMockito.doReturn(mockQuery).when(mockQuery).filter(anyString(), any());
    PowerMockito.doReturn(Collections.emptyMap()).when(mockWorkflowService).stencilMap(anyString());
    PowerMockito.whenNew(StateMachine.class).withAnyArguments().thenReturn(null);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void updateMetadtaWhenSameEnvironmentVariableUsedAgain() {
    HashMap<String, Object> metadataStored = new HashMap<>();
    metadataStored.put(Variable.RELATED_FIELD, "infra1");
    metadataStored.put(Variable.ENTITY_TYPE, ENVIRONMENT);

    List<Variable> pipelineVariables = new ArrayList<>();
    Variable envVarStored = aVariable().entityType(ENVIRONMENT).name("env").metadata(metadataStored).build();
    pipelineVariables.add(envVarStored);

    List<Variable> workflowVariables = new ArrayList<>();
    Variable envWorkflowVariable = aVariable()
                                       .entityType(ENVIRONMENT)
                                       .name("Environment")
                                       .metadata(ImmutableMap.of(Variable.RELATED_FIELD, "InfraDefinition_KUBERNETES",
                                           Variable.ENTITY_TYPE, ENVIRONMENT))
                                       .build();
    Variable infraWorkflowVariable = aVariable()
                                         .entityType(INFRASTRUCTURE_DEFINITION)
                                         .name("Infrastructure_KUBERNETES")
                                         .metadata(ImmutableMap.of(Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION))
                                         .build();
    workflowVariables.add(envWorkflowVariable);
    workflowVariables.add(infraWorkflowVariable);

    Map<String, String> pseWorkflowVariables = new HashMap<>();
    pseWorkflowVariables.put("Environment", "${env}");
    pseWorkflowVariables.put("Infrastructure_KUBERNETES", "${infra2}");
    pipelineServiceImpl.updateStoredVariable(
        pipelineVariables, true, false, workflowVariables, pseWorkflowVariables, envWorkflowVariable, "env");
    assertThat(envVarStored.getMetadata().get(Variable.RELATED_FIELD)).isEqualTo("infra1,infra2");
  }

  // When Service and infra both templatised in both phases, with same service var, but different infra var
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void updateMetadataServiceVarTC_1() {
    HashMap<String, Object> metadataStored = new HashMap<>();
    metadataStored.put(Variable.RELATED_FIELD, "infra1");
    metadataStored.put(Variable.ENTITY_TYPE, SERVICE);
    metadataStored.put(Variable.ARTIFACT_TYPE, "DOCKER");

    List<Variable> pipelineVariables = new ArrayList<>();
    Variable serviceVarStored = aVariable().entityType(SERVICE).name("srv").metadata(metadataStored).build();
    pipelineVariables.add(serviceVarStored);
    List<Variable> workflowVariables = new ArrayList<>();
    Variable serviceWorkflowVariable = aVariable()
                                           .entityType(SERVICE)
                                           .name("Service")
                                           .metadata(ImmutableMap.of(Variable.ARTIFACT_TYPE, "DOCKER",
                                               Variable.RELATED_FIELD, "infra2", Variable.ENTITY_TYPE, SERVICE))
                                           .build();
    Variable infraWorkflowVariable = aVariable()
                                         .entityType(INFRASTRUCTURE_DEFINITION)
                                         .name("Infrastructure_KUBERNETES")
                                         .metadata(ImmutableMap.of(Variable.RELATED_FIELD, "Service",
                                             Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION))
                                         .build();
    workflowVariables.add(serviceWorkflowVariable);
    workflowVariables.add(infraWorkflowVariable);

    Map<String, String> pseWorkflowVariables = new HashMap<>();
    pseWorkflowVariables.put("Service", "${srv}");
    pseWorkflowVariables.put("Infrastructure_KUBERNETES", "${infra2}");
    pipelineServiceImpl.updateStoredVariable(
        pipelineVariables, true, true, workflowVariables, pseWorkflowVariables, serviceWorkflowVariable, "srv");
    assertThat(serviceVarStored.getMetadata().get(Variable.RELATED_FIELD)).isEqualTo("infra1,infra2");
  }

  // When Service templatised in both phases, with same service var, infra templatised in 1, concrete in other
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void updateMetadataServiceVarTC_2() {
    HashMap<String, Object> metadataStored = new HashMap<>();
    metadataStored.put(Variable.INFRA_ID, "infra_id_1");
    metadataStored.put(Variable.ENTITY_TYPE, SERVICE);
    metadataStored.put(Variable.ARTIFACT_TYPE, "DOCKER");

    List<Variable> pipelineVariables = new ArrayList<>();
    Variable serviceVarStored = aVariable().entityType(SERVICE).name("srv").metadata(metadataStored).build();
    pipelineVariables.add(serviceVarStored);

    Variable serviceVariableNewPhase = aVariable()
                                           .entityType(SERVICE)
                                           .name("Service")
                                           .metadata(ImmutableMap.of(Variable.ARTIFACT_TYPE, "DOCKER",
                                               Variable.RELATED_FIELD, "infra2", Variable.ENTITY_TYPE, SERVICE))
                                           .build();

    pipelineServiceImpl.updateStoredVariable(
        pipelineVariables, true, true, new ArrayList<>(), new HashMap<>(), serviceVariableNewPhase, "srv");
    assertThat(serviceVarStored.getMetadata().get(Variable.RELATED_FIELD)).isEqualTo("infra2");
    assertThat(serviceVarStored.getMetadata().get(Variable.INFRA_ID)).isEqualTo("infra_id_1");
  }

  // When Service templatised in both phases, with same service var, infra concrete in both phases
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void updateMetadataServiceVarTC_3() {
    HashMap<String, Object> metadataStored = new HashMap<>();
    metadataStored.put(Variable.INFRA_ID, "infra_id_1");
    metadataStored.put(Variable.ENTITY_TYPE, SERVICE);
    metadataStored.put(Variable.ARTIFACT_TYPE, "DOCKER");

    List<Variable> pipelineVariables = new ArrayList<>();
    Variable serviceVarStored = aVariable().entityType(SERVICE).name("srv").metadata(metadataStored).build();
    pipelineVariables.add(serviceVarStored);

    Variable serviceVariableNewPhase = aVariable()
                                           .entityType(SERVICE)
                                           .name("Service")
                                           .metadata(ImmutableMap.of(Variable.ARTIFACT_TYPE, "DOCKER",
                                               Variable.INFRA_ID, "infra_id_2", Variable.ENTITY_TYPE, SERVICE))
                                           .build();
    pipelineServiceImpl.updateStoredVariable(
        pipelineVariables, true, true, new ArrayList<>(), new HashMap<>(), serviceVariableNewPhase, "srv");
    assertThat(serviceVarStored.getMetadata().get(Variable.INFRA_ID)).isEqualTo("infra_id_1,infra_id_2");
  }

  // When Service templatised in both phases, but different artifact type workflows
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void updateMetadataServiceVarTC_4() {
    HashMap<String, Object> metadataStored = new HashMap<>();
    metadataStored.put(Variable.INFRA_ID, "infra_id_1");
    metadataStored.put(Variable.ENTITY_TYPE, SERVICE);
    metadataStored.put(Variable.ARTIFACT_TYPE, "DOCKER");

    List<Variable> pipelineVariables = new ArrayList<>();
    Variable serviceVarStored = aVariable().entityType(SERVICE).name("srv").metadata(metadataStored).build();
    pipelineVariables.add(serviceVarStored);
    Variable serviceVariableNewPhase = aVariable()
                                           .entityType(SERVICE)
                                           .name("Service")
                                           .metadata(ImmutableMap.of(Variable.ARTIFACT_TYPE, "WAR",
                                               Variable.RELATED_FIELD, "infra2", Variable.ENTITY_TYPE, SERVICE))
                                           .build();

    assertThatThrownBy(()
                           -> pipelineServiceImpl.updateStoredVariable(pipelineVariables, true, true, new ArrayList<>(),
                               new HashMap<>(), serviceVariableNewPhase, "srv"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(
            "The same Workflow variable name srv cannot be used for Services using different Artifact types. Change the name of the variable in one or more Workflow.");
  }

  // When Infra templatised in both phases, but different envId selected
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void updateStoredVariableInfraTC_1() {
    HashMap<String, Object> metadataStored = new HashMap<>();
    metadataStored.put(Variable.ENV_ID, "env_id_1");
    metadataStored.put(Variable.SERVICE_ID, "s1");
    metadataStored.put(Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION);
    List<Variable> pipelineVariables = new ArrayList<>();
    Variable serviceVarStored =
        aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("infra").metadata(metadataStored).build();
    pipelineVariables.add(serviceVarStored);
    Variable infraVariableNewPhase = aVariable()
                                         .entityType(INFRASTRUCTURE_DEFINITION)
                                         .name("infra")
                                         .metadata(ImmutableMap.of(Variable.ENV_ID, "env_id_2", Variable.SERVICE_ID,
                                             "s2", Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION))
                                         .build();

    assertThatThrownBy(()
                           -> pipelineServiceImpl.updateStoredVariable(pipelineVariables, true, true, new ArrayList<>(),
                               new HashMap<>(), infraVariableNewPhase, "infra"))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(
            "The same Workflow variable name infra cannot be used for InfraDefinitions using different Environment. Change the name of the variable in one or more Workflow.");
  }

  // When Infra templatised in both phases, with same var,service templatised in both phases
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void updateStoredVariableInfraTC_2() {
    HashMap<String, Object> metadataStored = new HashMap<>();
    metadataStored.put(Variable.ENV_ID, "env_id_1");
    metadataStored.put(Variable.RELATED_FIELD, "srv1");
    metadataStored.put(Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION);
    List<Variable> pipelineVariables = new ArrayList<>();
    Variable infraVarStored =
        aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("infra").metadata(metadataStored).build();
    pipelineVariables.add(infraVarStored);
    Variable infraVariableNewPhase = aVariable()
                                         .entityType(INFRASTRUCTURE_DEFINITION)
                                         .name("infra")
                                         .metadata(ImmutableMap.of(Variable.ENV_ID, "env_id_1", Variable.RELATED_FIELD,
                                             "srv2", Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION))
                                         .build();

    pipelineServiceImpl.updateStoredVariable(
        pipelineVariables, true, true, new ArrayList<>(), new HashMap<>(), infraVariableNewPhase, "infra");
    assertThat(infraVarStored.getMetadata().get(Variable.RELATED_FIELD)).isEqualTo("srv1,srv2");
  }

  // When Infra templatised in both phases, with same var,service templatised in one phase, concrete in another
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void updateStoredVariableInfraTC_3() {
    HashMap<String, Object> metadataStored = new HashMap<>();
    metadataStored.put(Variable.ENV_ID, "env_id_1");
    metadataStored.put(Variable.RELATED_FIELD, "srv1");
    metadataStored.put(Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION);
    List<Variable> pipelineVariables = new ArrayList<>();
    Variable infraVarStored =
        aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("infra").metadata(metadataStored).build();
    pipelineVariables.add(infraVarStored);
    Variable infraVariableNewPhase = aVariable()
                                         .entityType(INFRASTRUCTURE_DEFINITION)
                                         .name("infra")
                                         .metadata(ImmutableMap.of(Variable.ENV_ID, "env_id_1", Variable.SERVICE_ID,
                                             "service_id_2", Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION))
                                         .build();

    pipelineServiceImpl.updateStoredVariable(
        pipelineVariables, true, true, new ArrayList<>(), new HashMap<>(), infraVariableNewPhase, "infra");
    assertThat(infraVarStored.getMetadata().get(Variable.RELATED_FIELD)).isEqualTo("srv1");
    assertThat(infraVarStored.getMetadata().get(Variable.SERVICE_ID)).isEqualTo("service_id_2");
  }

  // When Infra templatised in both phases, with same var,service concrete in both
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void updateStoredVariableInfraTC_4() {
    HashMap<String, Object> metadataStored = new HashMap<>();
    metadataStored.put(Variable.ENV_ID, "env_id_1");
    metadataStored.put(Variable.SERVICE_ID, "serviceID_1,serviceID_2");
    metadataStored.put(Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION);
    List<Variable> pipelineVariables = new ArrayList<>();
    Variable infraVarStored =
        aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("infra").metadata(metadataStored).build();
    pipelineVariables.add(infraVarStored);
    Variable infraVariableNewPhase = aVariable()
                                         .entityType(INFRASTRUCTURE_DEFINITION)
                                         .name("infra")
                                         .metadata(ImmutableMap.of(Variable.ENV_ID, "env_id_1", Variable.SERVICE_ID,
                                             "serviceID_2", Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION))
                                         .build();

    pipelineServiceImpl.updateStoredVariable(
        pipelineVariables, true, true, new ArrayList<>(), new HashMap<>(), infraVariableNewPhase, "infra");
    assertThat(infraVarStored.getMetadata().get(Variable.SERVICE_ID)).isEqualTo("serviceID_1,serviceID_2");
  }

  // When two phase has same env variables, Service non templatised, and infra variable with different vars
  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void setPipelineVariablesTC_1() {
    HashMap<String, Object> metadataEnv = new HashMap<>();
    metadataEnv.put(Variable.RELATED_FIELD, "InfraDefinition_ECS");
    metadataEnv.put(Variable.ENTITY_TYPE, ENVIRONMENT);

    Variable envVarWorkflow = aVariable().entityType(ENVIRONMENT).name("Environment").metadata(metadataEnv).build();

    HashMap<String, Object> metadataInfra = new HashMap<>();
    metadataInfra.put(Variable.SERVICE_ID, "s1");
    metadataInfra.put(Variable.ENTITY_TYPE, INFRASTRUCTURE_DEFINITION);
    Variable infraVarWorkflow =
        aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("InfraDefinition_ECS").metadata(metadataInfra).build();
    List<Variable> userVariables = new ArrayList<>();
    userVariables.add(envVarWorkflow);
    userVariables.add(infraVarWorkflow);

    Workflow workflow =
        aWorkflow()
            .orchestrationWorkflow(aCanaryOrchestrationWorkflow().withUserVariables(userVariables).build())
            .accountId("ACCOUNT_ID")
            .build();

    List<Variable> pipelineVariables = new ArrayList<>();

    Map<String, String> pseWorkflowVariables = new HashMap<>();
    pseWorkflowVariables.put("Environment", "${env}");
    pseWorkflowVariables.put("InfraDefinition_ECS", "${infra1}");

    PipelineStageElement pse = PipelineStageElement.builder().workflowVariables(pseWorkflowVariables).build();

    Map<String, String> pse2WorkflowVariables = new HashMap<>();
    pse2WorkflowVariables.put("Environment", "${env}");
    pse2WorkflowVariables.put("InfraDefinition_ECS", "${infra2}");
    PipelineStageElement pse2 = PipelineStageElement.builder().workflowVariables(pse2WorkflowVariables).build();

    pipelineServiceImpl.setPipelineVariables(workflow, pse, pipelineVariables, false, true);
    pipelineServiceImpl.setPipelineVariables(workflow, pse2, pipelineVariables, false, true);

    assertThat(pipelineVariables).isNotEmpty();
    assertThat(pipelineVariables.get(0).getName()).isEqualTo("env");
    assertThat(pipelineVariables.get(0).getMetadata().get(Variable.RELATED_FIELD)).isEqualTo("infra1,infra2");
  }
}