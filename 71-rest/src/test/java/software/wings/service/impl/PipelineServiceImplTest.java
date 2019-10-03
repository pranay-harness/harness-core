package software.wings.service.impl;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.EntityType.APPDYNAMICS_APPID;
import static software.wings.beans.EntityType.APPDYNAMICS_CONFIGID;
import static software.wings.beans.EntityType.APPDYNAMICS_TIERID;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_DEFINITION;
import static software.wings.beans.EntityType.INFRASTRUCTURE_MAPPING;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.Variable.VariableBuilder.aVariable;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PipelineServiceImplTest extends WingsBaseTest {
  @Inject @InjectMocks private PipelineServiceImpl pipelineServiceImpl;

  @Test
  @Category(UnitTests.class)
  public void testPopulateParentFields() {
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
        aVariable().entityType(APPDYNAMICS_TIERID).name("AppdTierId").value("${Tier}").build(),
        aVariable().type(VariableType.TEXT).name("test").value("test").build());

    Map<String, String> pseWorkflowVariables = new HashMap<>();
    pseWorkflowVariables.put("Service", "Service 2");
    pseWorkflowVariables.put("Environment", "Environment 2");
    pseWorkflowVariables.put("ServiceInfra_ECS", "{$infra}");
    pseWorkflowVariables.put("AppdAppId", "AppD app 1");
    pseWorkflowVariables.put("AppdConfigId", "AppD config 2");

    pipelineServiceImpl.populateParentFields(
        infraMappingPipelineVar, INFRASTRUCTURE_MAPPING, workflowVariables, "ServiceInfra_ECS", pseWorkflowVariables);
    assertThat(infraMappingPipelineVar.getMetadata().get(Variable.ENV_ID)).isNotNull();
    assertThat(infraMappingPipelineVar.getMetadata().get(Variable.ENV_ID)).isEqualTo("Environment 2");

    assertThat(infraMappingPipelineVar.getMetadata().get(Variable.SERVICE_ID)).isNotNull();
    assertThat(infraMappingPipelineVar.getMetadata().get(Variable.SERVICE_ID)).isEqualTo("Service 2");

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

    pipelineServiceImpl.populateParentFields(
        infraDefPipelineVar, INFRASTRUCTURE_DEFINITION, workflowVariables, "ServiceInfra_ECS", pseWorkflowVariables);
    assertThat(infraDefPipelineVar.getMetadata().get(Variable.ENV_ID)).isNotNull();
    assertThat(infraDefPipelineVar.getMetadata().get(Variable.ENV_ID)).isEqualTo("Environment 2");

    assertThat(infraDefPipelineVar.getMetadata().get(Variable.SERVICE_ID)).isNotNull();
    assertThat(infraDefPipelineVar.getMetadata().get(Variable.SERVICE_ID)).isEqualTo("Service 2");

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
        appdTierPipelineVar, APPDYNAMICS_TIERID, workflowVariables, "AppdTierId", pseWorkflowVariables);
    assertThat(appdTierPipelineVar.getMetadata().get(Variable.PARENT_FIELDS)).isNotNull();
    Map<String, String> parents = (Map<String, String>) appdTierPipelineVar.getMetadata().get(Variable.PARENT_FIELDS);
    assertThat(parents.get("applicationId")).isEqualTo("AppD app 1");
    assertThat(parents.get("analysisServerConfigId")).isEqualTo("AppD config 2");
  }
}