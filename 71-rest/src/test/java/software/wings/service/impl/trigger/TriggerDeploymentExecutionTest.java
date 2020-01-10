package software.wings.service.impl.trigger;

import static io.harness.rule.OwnerRule.HARSH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.api.DeploymentType.SSH;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.INFRASTRUCTURE_DEFINITION;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_DEFINITION_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;
import static software.wings.utils.WingsTestConstants.INFRA_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Variable;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerDeploymentExecutionTest extends WingsBaseTest {
  @Mock private InfrastructureDefinitionService infrastructureDefinitionService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Inject @InjectMocks private TriggerDeploymentExecution triggerDeploymentExecution;

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldGetInfraMappingByName() {
    InfrastructureMapping infrastructureMappingMocked = anAwsInfrastructureMapping()
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withDeploymentType(SSH.name())
                                                            .withServiceId(SERVICE_ID)

                                                            .withComputeProviderType("AWS")
                                                            .build();
    when(infrastructureMappingService.getInfraMappingByName(APP_ID, ENV_ID, INFRA_NAME))
        .thenReturn(infrastructureMappingMocked);

    InfrastructureMapping infrastructureMapping =
        triggerDeploymentExecution.getInfrastructureMapping(APP_ID, ENV_ID, INFRA_NAME);
    assertThat(infrastructureMapping).isNotNull();
    assertThat(infrastructureMapping).isEqualTo(infrastructureMappingMocked);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldGetInfraDefinitionByName() {
    InfrastructureDefinition infrastructureDefinitionMocked =
        InfrastructureDefinition.builder()
            .infrastructure(GoogleKubernetesEngine.builder().namespace("test").build())
            .build();
    when(infrastructureDefinitionService.getInfraDefByName(APP_ID, ENV_ID, INFRA_NAME))
        .thenReturn(infrastructureDefinitionMocked);

    InfrastructureDefinition infrastructureDefinition =
        triggerDeploymentExecution.getInfrastructureDefinition(APP_ID, ENV_ID, INFRA_NAME);
    assertThat(infrastructureDefinition).isNotNull();
    assertThat(infrastructureDefinition).isEqualTo(infrastructureDefinitionMocked);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldResolveInfraDefOldPayloadCustomName() {
    InfrastructureMapping infrastructureMappingMocked = anAwsInfrastructureMapping()
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withServiceId(SERVICE_ID)
                                                            .withDeploymentType(SSH.name())
                                                            .withComputeProviderType("AWS")
                                                            .build();

    infrastructureMappingMocked.setInfrastructureDefinitionId(INFRA_DEFINITION_ID);

    when(infrastructureMappingService.getInfraMappingByName(APP_ID, ENV_ID, INFRA_NAME))
        .thenReturn(infrastructureMappingMocked);

    List<Variable> workflowVariables =
        asList(aVariable().entityType(SERVICE).name("Service").value("Service 1").build(),
            aVariable().entityType(ENVIRONMENT).name("Environment").value("env 1").build(),
            aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("Infra").value("${Infra}").build());

    Map<String, String> triggerWorkflowVariables = new HashMap<>();
    triggerWorkflowVariables.put("Environment", ENV_NAME);
    triggerWorkflowVariables.put("Service", SERVICE_NAME);
    triggerWorkflowVariables.put("Infra", INFRA_NAME);

    triggerDeploymentExecution.resolveInfraDefinitions(APP_ID, triggerWorkflowVariables, ENV_ID, workflowVariables);
    assertThat(triggerWorkflowVariables.get("Infra")).isNotNull();
    assertThat(triggerWorkflowVariables.get("Infra")).isEqualTo(INFRA_DEFINITION_ID);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldResolveInfraDefNewPayloadCustomName() {
    List<Variable> workflowVariables =
        asList(aVariable().entityType(SERVICE).name("Service").value("Service 1").build(),
            aVariable().entityType(ENVIRONMENT).name("Environment").value("env 1").build(),
            aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("Infra").value("${Infra}").build());

    Map<String, String> triggerWorkflowVariables = new HashMap<>();
    triggerWorkflowVariables.put("Environment", ENV_NAME);
    triggerWorkflowVariables.put("Service", SERVICE_NAME);
    triggerWorkflowVariables.put("Infra", INFRA_NAME);

    InfrastructureDefinition infrastructureDefinitionMocked =
        InfrastructureDefinition.builder()
            .uuid(INFRA_DEFINITION_ID)
            .infrastructure(GoogleKubernetesEngine.builder().namespace("test").build())
            .build();
    when(infrastructureDefinitionService.getInfraDefByName(APP_ID, ENV_ID, INFRA_NAME))
        .thenReturn(infrastructureDefinitionMocked);

    triggerDeploymentExecution.resolveInfraDefinitions(APP_ID, triggerWorkflowVariables, ENV_ID, workflowVariables);
    assertThat(triggerWorkflowVariables.get("Infra")).isNotNull();
    assertThat(triggerWorkflowVariables.get("Infra")).isEqualTo(INFRA_DEFINITION_ID);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldResolveInfraDefNewPayloadAutoGeneratedName() {
    List<Variable> workflowVariables =
        asList(aVariable().entityType(SERVICE).name("Service").value("Service 1").build(),
            aVariable().entityType(ENVIRONMENT).name("Environment").value("env 1").build(),
            aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("InfraDefinition_ECS").value("${Infra}").build());

    Map<String, String> triggerWorkflowVariables = new HashMap<>();
    triggerWorkflowVariables.put("Environment", ENV_NAME);
    triggerWorkflowVariables.put("Service", SERVICE_NAME);
    triggerWorkflowVariables.put("InfraDefinition_ECS", INFRA_NAME);

    InfrastructureDefinition infrastructureDefinitionMocked =
        InfrastructureDefinition.builder()
            .uuid(INFRA_DEFINITION_ID)
            .infrastructure(GoogleKubernetesEngine.builder().namespace("test").build())
            .build();
    when(infrastructureDefinitionService.getInfraDefByName(APP_ID, ENV_ID, INFRA_NAME))
        .thenReturn(infrastructureDefinitionMocked);

    triggerDeploymentExecution.resolveInfraDefinitions(APP_ID, triggerWorkflowVariables, ENV_ID, workflowVariables);
    assertThat(triggerWorkflowVariables.get("InfraDefinition_ECS")).isNotNull();
    assertThat(triggerWorkflowVariables.get("InfraDefinition_ECS")).isEqualTo(INFRA_DEFINITION_ID);
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldResolveInfraDefOldPayloadAutoGeneratedName() {
    Map<String, String> triggerWorkflowVariables = new HashMap<>();
    triggerWorkflowVariables.put("Environment", ENV_NAME);
    triggerWorkflowVariables.put("Service", SERVICE_NAME);
    triggerWorkflowVariables.put("ServiceInfra_ECS", INFRA_NAME);
    triggerWorkflowVariables.put("InfraDefinition_ECS", "${Infra}");

    InfrastructureMapping infrastructureMappingMocked = anAwsInfrastructureMapping()
                                                            .withUuid(INFRA_MAPPING_ID)
                                                            .withServiceId(SERVICE_ID)
                                                            .withDeploymentType(SSH.name())
                                                            .withComputeProviderType("AWS")
                                                            .build();
    infrastructureMappingMocked.setInfrastructureDefinitionId(INFRA_DEFINITION_ID);

    when(infrastructureMappingService.getInfraMappingByName(APP_ID, ENV_ID, INFRA_NAME))
        .thenReturn(infrastructureMappingMocked);

    List<Variable> workflowVariables =
        asList(aVariable().entityType(SERVICE).name("Service").value("Service 1").build(),
            aVariable().entityType(ENVIRONMENT).name("Environment").value("env 1").build(),
            aVariable().entityType(INFRASTRUCTURE_DEFINITION).name("InfraDefinition_ECS").value("${Infra}").build());

    triggerDeploymentExecution.resolveInfraDefinitions(APP_ID, triggerWorkflowVariables, ENV_ID, workflowVariables);
    assertThat(triggerWorkflowVariables.get("InfraDefinition_ECS")).isNotNull();
    assertThat(triggerWorkflowVariables.get("InfraDefinition_ECS")).isEqualTo(INFRA_DEFINITION_ID);
  }
}
