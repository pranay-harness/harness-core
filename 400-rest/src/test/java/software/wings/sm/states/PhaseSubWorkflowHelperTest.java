/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static software.wings.utils.WingsTestConstants.APP_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Environment;
import software.wings.beans.GcpKubernetesInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.TemplateExpression;
import software.wings.common.TemplateExpressionProcessor;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.PhaseSubWorkflowHelperService;
import software.wings.service.intfc.InfrastructureDefinitionService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionContext;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class PhaseSubWorkflowHelperTest extends WingsBaseTest {
  private static final String SERVICE_NAME_FOR_NON_TEMPLATIZED = "SERVICE_FOR_NON_TEMPLATIZED";
  private static final String SERVICE_NAME_FOR_TEMPLATIZED = "SERVICE_FOR_TEMPLATIZED";
  private static final String SERVICE_ID = "SERVICE_ID";
  private static final String SERVICE_ID_EXPRESSION = "SERVICE_ID_EXPRESSION";
  private static final String INFRA_DEF_NAME_FOR_NON_TEMPLATIZED = "INFRA_DEF_NAME_FOR_NON_TEMPLATIZED";
  private static final String INFRA_DEFINITION_ID = "INFRA_DEFINITION_ID";
  private static final String INFRA_DEF_NAME_FOR_TEMPLATIZED = "INFRA_DEF_NAME_FOR_TEMPLATIZED";
  private static final String INFRA_ID_EXPRESSION = "INFRA_ID_EXPRESSION";
  public static final String ENV_ID = "ENV_ID";
  private static final String WRONG_ENV_ID = "WRONG_ENV_ID";
  private static final String WRONG_ID = "WRONG_ID";
  public static final String ACCOUNT_ID = "ACCOUNT_ID";

  @Mock ServiceResourceService serviceResourceService;
  @Mock InfrastructureDefinitionService infrastructureDefinitionService;
  @Mock TemplateExpressionProcessor templateExpressionProcessor;
  @Mock FeatureFlagService featureFlagService;
  @InjectMocks @Inject PhaseSubWorkflowHelperService phaseSubWorkflowHelperService;
  ExecutionContext context;

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldGetServiceForNonTemplatized() {
    Service serviceForNonTemplatized = Service.builder().name(SERVICE_NAME_FOR_NON_TEMPLATIZED).build();
    when(serviceResourceService.get(APP_ID, SERVICE_ID)).thenReturn(serviceForNonTemplatized);

    Service service = phaseSubWorkflowHelperService.getService(SERVICE_ID, null, APP_ID, context);

    assertThat(service).isNotNull();
    assertThat(SERVICE_NAME_FOR_NON_TEMPLATIZED).isEqualTo(service.getName());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldGetServiceForTemplatized() {
    TemplateExpression serviceTemplateExpression =
        TemplateExpression.builder().fieldName(PhaseSubWorkflow.SERVICE_ID).expression(SERVICE_ID_EXPRESSION).build();
    Service serviceForTemplatized = Service.builder().name(SERVICE_NAME_FOR_TEMPLATIZED).build();
    when(templateExpressionProcessor.resolveService(context, APP_ID, serviceTemplateExpression))
        .thenReturn(serviceForTemplatized);

    Service service = phaseSubWorkflowHelperService.getService(SERVICE_ID, serviceTemplateExpression, APP_ID, context);

    assertThat(service).isNotNull();
    assertThat(SERVICE_NAME_FOR_TEMPLATIZED).isEqualTo(service.getName());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldNotGetServiceForBuildWorkflow() {
    Service service = phaseSubWorkflowHelperService.getService(null, null, APP_ID, context);

    assertThat(service).isNull();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldGetInfraDefinitionForNonTemplatized() {
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder().name(INFRA_DEF_NAME_FOR_NON_TEMPLATIZED).build();
    when(infrastructureDefinitionService.get(APP_ID, INFRA_DEFINITION_ID)).thenReturn(infrastructureDefinition);

    InfrastructureDefinition infraDefinition =
        phaseSubWorkflowHelperService.getInfraDefinition(INFRA_DEFINITION_ID, null, APP_ID, context);

    assertThat(infraDefinition).isNotNull();
    assertThat(INFRA_DEF_NAME_FOR_NON_TEMPLATIZED).isEqualTo(infraDefinition.getName());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldGetInfraDefinitionForTemplatized() {
    TemplateExpression infraDefTemplateExpression = TemplateExpression.builder()
                                                        .fieldName(PhaseSubWorkflow.INFRA_DEFINITION_ID)
                                                        .expression(INFRA_ID_EXPRESSION)
                                                        .build();
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder().name(INFRA_DEF_NAME_FOR_TEMPLATIZED).build();
    when(templateExpressionProcessor.resolveInfraDefinition(context, APP_ID, infraDefTemplateExpression))
        .thenReturn(infrastructureDefinition);

    InfrastructureDefinition infraDefinition = phaseSubWorkflowHelperService.getInfraDefinition(
        INFRA_DEFINITION_ID, infraDefTemplateExpression, APP_ID, context);

    assertThat(infraDefinition).isNotNull();
    assertThat(INFRA_DEF_NAME_FOR_TEMPLATIZED).isEqualTo(infraDefinition.getName());
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldNotGetInfraDefinitionForBuildWorkflow() {
    InfrastructureDefinition infraDefinition =
        phaseSubWorkflowHelperService.getInfraDefinition(null, null, APP_ID, context);

    assertThat(infraDefinition).isNull();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldValidateEnvInfraRelationship() {
    Environment environment = Environment.Builder.anEnvironment().uuid(ENV_ID).build();
    InfrastructureDefinition infraDefinition = InfrastructureDefinition.builder().envId(ENV_ID).build();
    InfrastructureMapping infrastructureMapping = GcpKubernetesInfrastructureMapping.builder().envId(ENV_ID).build();
    phaseSubWorkflowHelperService.validateEnvInfraRelationShip(environment, infraDefinition, infrastructureMapping);

    infraDefinition.setEnvId(WRONG_ENV_ID);
    assertThatThrownBy(()
                           -> phaseSubWorkflowHelperService.validateEnvInfraRelationShip(
                               environment, infraDefinition, infrastructureMapping))
        .isInstanceOf(InvalidRequestException.class);
    infraDefinition.setEnvId(ENV_ID);
    phaseSubWorkflowHelperService.validateEnvInfraRelationShip(environment, infraDefinition, infrastructureMapping);

    infrastructureMapping.setEnvId(WRONG_ENV_ID);
    assertThatThrownBy(()
                           -> phaseSubWorkflowHelperService.validateEnvInfraRelationShip(
                               environment, infraDefinition, infrastructureMapping))
        .isInstanceOf(InvalidRequestException.class);
    infrastructureMapping.setEnvId(ENV_ID);
    phaseSubWorkflowHelperService.validateEnvInfraRelationShip(environment, infraDefinition, infrastructureMapping);

    // Validate doesn't fail for null values
    phaseSubWorkflowHelperService.validateEnvInfraRelationShip(null, null, null);
    phaseSubWorkflowHelperService.validateEnvInfraRelationShip(environment, null, null);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldValidateServiceInfraMappingRelationship() {
    Service service = Service.builder().uuid(SERVICE_ID).build();
    InfrastructureMapping infrastructureMapping =
        GcpKubernetesInfrastructureMapping.builder().serviceId(SERVICE_ID).build();
    phaseSubWorkflowHelperService.validateServiceInfraMappingRelationShip(service, infrastructureMapping);

    infrastructureMapping.setServiceId(WRONG_ID);
    assertThatThrownBy(
        () -> phaseSubWorkflowHelperService.validateServiceInfraMappingRelationShip(service, infrastructureMapping));
    infrastructureMapping.setServiceId(SERVICE_ID);
    phaseSubWorkflowHelperService.validateServiceInfraMappingRelationShip(service, infrastructureMapping);

    // Validate should not fail for null values
    phaseSubWorkflowHelperService.validateServiceInfraMappingRelationShip(null, null);
    phaseSubWorkflowHelperService.validateServiceInfraMappingRelationShip(service, null);
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void shouldValidateScopedService() {
    Service service = Service.builder().uuid(SERVICE_ID).build();
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder().scopedToServices(Collections.singletonList(SERVICE_ID)).build();
    phaseSubWorkflowHelperService.validateScopedServices(service, infrastructureDefinition);

    infrastructureDefinition.setScopedToServices(Collections.singletonList(WRONG_ID));
    assertThatThrownBy(() -> phaseSubWorkflowHelperService.validateScopedServices(service, infrastructureDefinition));
    infrastructureDefinition.setScopedToServices(Collections.singletonList(SERVICE_ID));
    phaseSubWorkflowHelperService.validateScopedServices(service, infrastructureDefinition);

    // Multiple elements in the list
    infrastructureDefinition.setScopedToServices(Arrays.asList(WRONG_ID, SERVICE_ID));
    phaseSubWorkflowHelperService.validateScopedServices(service, infrastructureDefinition);

    // Should not fail for null  values
    phaseSubWorkflowHelperService.validateScopedServices(null, infrastructureDefinition);
    phaseSubWorkflowHelperService.validateScopedServices(service, null);
  }
}
