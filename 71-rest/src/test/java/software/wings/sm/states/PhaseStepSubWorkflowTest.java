package software.wings.sm.states;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.spy;

import io.harness.category.element.UnitTests;
import io.harness.context.ContextElementType;
import io.harness.rule.Owner;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.PhaseElement;
import software.wings.api.ServiceElement;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.TerraformInfrastructureProvisioner;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionContext;

public class PhaseStepSubWorkflowTest extends WingsBaseTest {
  private static final String INFRA_DEFINITION_ID = "INFRA_DEFINITION_ID";
  public static final String SERVICE_ID = "SERVICE_ID";
  public static final String APP_ID = "APP_ID";
  public static final String INFRA_MAPPING_ID = "INFRA_MAPPING_ID";
  private static final String PROVISIONER_ID = "PROVISIONER_ID";
  private static final String RANDOM = "RANDOM";

  @Mock private ExecutionContext executionContext;
  @Mock private InfrastructureDefinitionService infrastructureDefinitionService;
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private InfrastructureMappingService infrastructureMappingService;
  @Mock private InfrastructureProvisionerService infrastructureProvisionerService;

  private PhaseStepSubWorkflow phaseStepSubWorkflow = spy(new PhaseStepSubWorkflow(RANDOM));

  @Before
  public void setUp() throws Exception {
    Reflect.on(phaseStepSubWorkflow).set("infrastructureDefinitionService", infrastructureDefinitionService);
    Reflect.on(phaseStepSubWorkflow).set("workflowExecutionService", workflowExecutionService);
    Reflect.on(phaseStepSubWorkflow).set("infrastructureMappingService", infrastructureMappingService);
    Reflect.on(phaseStepSubWorkflow).set("infrastructureProvisionerService", infrastructureProvisionerService);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldNotCallGetInfraMappingWhenAlreadyAvailable() {
    when(executionContext.fetchInfraMappingId()).thenReturn("INFRA_MAPPING_ID");

    phaseStepSubWorkflow.populateInfraMapping(executionContext);

    verify(infrastructureDefinitionService, never()).renderAndSaveInfraMapping(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldCreateInfraMappingWhenNonProvisioner() {
    doReturn(null).when(executionContext).fetchInfraMappingId();
    doReturn(APP_ID).when(executionContext).getAppId();
    PhaseElement phaseElement = PhaseElement.builder()
                                    .infraDefinitionId(INFRA_DEFINITION_ID)
                                    .serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build())
                                    .build();
    doReturn(phaseElement).when(executionContext).getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    InfrastructureDefinition infraDefinition = InfrastructureDefinition.builder().build();
    doReturn(infraDefinition).when(infrastructureDefinitionService).get(APP_ID, INFRA_DEFINITION_ID);
    InfrastructureMapping infraMapping = GcpKubernetesInfrastructureMapping.builder().uuid(INFRA_MAPPING_ID).build();
    doReturn(infraMapping)
        .when(infrastructureDefinitionService)
        .renderAndSaveInfraMapping(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID, executionContext);
    doNothing()
        .when(phaseStepSubWorkflow)
        .updateInfraMappingDependencies(executionContext, phaseElement, APP_ID, infraMapping);

    phaseStepSubWorkflow.populateInfraMapping(executionContext);

    verify(infrastructureDefinitionService, times(1))
        .renderAndSaveInfraMapping(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID, executionContext);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldNotCreateInfraMappingWhenProvisionerOutputsNotAvailable() {
    doReturn(null).when(executionContext).fetchInfraMappingId();
    doReturn(APP_ID).when(executionContext).getAppId();
    PhaseElement phaseElement = PhaseElement.builder()
                                    .infraDefinitionId(INFRA_DEFINITION_ID)
                                    .serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build())
                                    .build();
    doReturn(phaseElement).when(executionContext).getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    InfrastructureDefinition infraDefinition = InfrastructureDefinition.builder().provisionerId(PROVISIONER_ID).build();
    doReturn(infraDefinition).when(infrastructureDefinitionService).get(APP_ID, INFRA_DEFINITION_ID);
    InfrastructureProvisioner infraProvisioner = TerraformInfrastructureProvisioner.builder().build();
    doReturn(infraProvisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);
    doReturn(null).when(executionContext).evaluateExpression(any());

    phaseStepSubWorkflow.populateInfraMapping(executionContext);

    verify(infrastructureDefinitionService, never())
        .renderAndSaveInfraMapping(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID, executionContext);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldCreateInfraMappingWhenProvisionerOutputsAvailable() {
    doReturn(null).when(executionContext).fetchInfraMappingId();
    doReturn(APP_ID).when(executionContext).getAppId();
    PhaseElement phaseElement = PhaseElement.builder()
                                    .infraDefinitionId(INFRA_DEFINITION_ID)
                                    .serviceElement(ServiceElement.builder().uuid(SERVICE_ID).build())
                                    .build();
    doReturn(phaseElement).when(executionContext).getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);
    InfrastructureDefinition infraDefinition = InfrastructureDefinition.builder().provisionerId(PROVISIONER_ID).build();
    doReturn(infraDefinition).when(infrastructureDefinitionService).get(APP_ID, INFRA_DEFINITION_ID);
    InfrastructureProvisioner infraProvisioner = TerraformInfrastructureProvisioner.builder().build();
    doReturn(infraProvisioner).when(infrastructureProvisionerService).get(APP_ID, PROVISIONER_ID);
    doReturn(RANDOM).when(executionContext).evaluateExpression(any());
    InfrastructureMapping infraMapping = GcpKubernetesInfrastructureMapping.builder().uuid(INFRA_MAPPING_ID).build();
    doReturn(infraMapping)
        .when(infrastructureDefinitionService)
        .renderAndSaveInfraMapping(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID, executionContext);
    doNothing()
        .when(phaseStepSubWorkflow)
        .updateInfraMappingDependencies(executionContext, phaseElement, APP_ID, infraMapping);

    phaseStepSubWorkflow.populateInfraMapping(executionContext);

    verify(infrastructureDefinitionService, times(1))
        .renderAndSaveInfraMapping(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID, executionContext);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldNotCreateInfraMappingForBuildWorkflow() {
    doReturn(null).when(executionContext).fetchInfraMappingId();
    doReturn(APP_ID).when(executionContext).getAppId();
    PhaseElement phaseElement = PhaseElement.builder().build();
    doReturn(phaseElement).when(executionContext).getContextElement(ContextElementType.PARAM, PhaseElement.PHASE_PARAM);

    phaseStepSubWorkflow.populateInfraMapping(executionContext);

    verify(infrastructureDefinitionService, never())
        .renderAndSaveInfraMapping(APP_ID, SERVICE_ID, INFRA_DEFINITION_ID, executionContext);
  }
}