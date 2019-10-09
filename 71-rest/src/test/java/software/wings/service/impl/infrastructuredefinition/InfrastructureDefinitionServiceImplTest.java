package software.wings.service.impl.infrastructuredefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static software.wings.common.InfrastructureConstants.INFRA_KUBERNETES_INFRAID_EXPRESSION;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.infra.GoogleKubernetesEngine;
import software.wings.infra.InfrastructureDefinition;
import software.wings.sm.ExecutionContext;

public class InfrastructureDefinitionServiceImplTest extends WingsBaseTest {
  @Mock private ExecutionContext executionContext;

  @InjectMocks private InfrastructureDefinitionServiceImpl infrastructureDefinitionService;

  @Test
  @Category(UnitTests.class)
  public void shouldFailOnNonResolutionOfExpressions() {
    String wrongVariable = "${WRONG_VARIABLE}";
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .infrastructure(GoogleKubernetesEngine.builder().namespace(wrongVariable).build())
            .build();
    when(executionContext.renderExpression(wrongVariable)).thenReturn(wrongVariable);

    assertThatThrownBy(
        () -> infrastructureDefinitionService.renderExpression(infrastructureDefinition, executionContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(wrongVariable);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldFailOnNullRenderedValueOfExpression() {
    String wrongVariable = "${WRONG_VARIABLE}";
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .infrastructure(GoogleKubernetesEngine.builder().namespace(wrongVariable).build())
            .build();
    when(executionContext.renderExpression(wrongVariable)).thenReturn(null);

    assertThatThrownBy(
        () -> infrastructureDefinitionService.renderExpression(infrastructureDefinition, executionContext))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining(wrongVariable);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldResolveExpressions() {
    String workflowVariable = "abc-${workflow.variables.var}";
    String value = "value";
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .infrastructure(GoogleKubernetesEngine.builder().namespace(workflowVariable).build())
            .build();
    when(executionContext.renderExpression(workflowVariable)).thenReturn(value);

    infrastructureDefinitionService.renderExpression(infrastructureDefinition, executionContext);

    assertThat(((GoogleKubernetesEngine) infrastructureDefinition.getInfrastructure()).getNamespace()).isEqualTo(value);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldIgnoreReleaseNameResolutionFailure() {
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .infrastructure(GoogleKubernetesEngine.builder().releaseName(INFRA_KUBERNETES_INFRAID_EXPRESSION).build())
            .build();
    when(executionContext.renderExpression(INFRA_KUBERNETES_INFRAID_EXPRESSION))
        .thenReturn(INFRA_KUBERNETES_INFRAID_EXPRESSION);

    infrastructureDefinitionService.renderExpression(infrastructureDefinition, executionContext);

    assertThat(((GoogleKubernetesEngine) infrastructureDefinition.getInfrastructure()).getReleaseName())
        .isEqualTo(INFRA_KUBERNETES_INFRAID_EXPRESSION);
  }

  @Test
  @Category(UnitTests.class)
  public void shouldIgnoreReleaseNameWithSomeConstantResolutionFailure() {
    String releaseName = "release-" + INFRA_KUBERNETES_INFRAID_EXPRESSION;
    InfrastructureDefinition infrastructureDefinition =
        InfrastructureDefinition.builder()
            .infrastructure(GoogleKubernetesEngine.builder().releaseName(releaseName).build())
            .build();
    when(executionContext.renderExpression(releaseName)).thenReturn(releaseName);

    infrastructureDefinitionService.renderExpression(infrastructureDefinition, executionContext);

    assertThat(((GoogleKubernetesEngine) infrastructureDefinition.getInfrastructure()).getReleaseName())
        .isEqualTo(releaseName);
  }
}