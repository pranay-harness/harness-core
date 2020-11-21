package software.wings.helpers.ext.helm.request;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.service.impl.ContainerServiceParams;

import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class HelmCommandRequestTest extends WingsBaseTest {
  @Mock private ContainerServiceParams containerServiceParams;

  @Before
  public void setUp() throws Exception {
    doReturn(asList(HttpConnectionExecutionCapability.builder().build()))
        .when(containerServiceParams)
        .fetchRequiredExecutionCapabilities();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void fetchRequiredExecutionCapabilities() {
    testWithoutContainerParams();
    testWithoutGitConfig();
    testWithContainerParams();
  }

  private void testWithoutGitConfig() {
    HelmRollbackCommandRequest rollbackCommandRequest =
        HelmRollbackCommandRequest.builder().containerServiceParams(containerServiceParams).build();
    assertThat(rollbackCommandRequest.fetchRequiredExecutionCapabilities()
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactly(CapabilityType.HELM_COMMAND, CapabilityType.HTTP);
  }

  private void testWithContainerParams() {
    HelmInstallCommandRequest installCommandRequest = HelmInstallCommandRequest.builder()
                                                          .gitConfig(new GitConfig())
                                                          .containerServiceParams(containerServiceParams)
                                                          .build();
    assertThat(installCommandRequest.fetchRequiredExecutionCapabilities()
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactly(CapabilityType.HELM_COMMAND, CapabilityType.GIT_CONNECTION, CapabilityType.HTTP);
  }

  private void testWithoutContainerParams() {
    HelmInstallCommandRequest installCommandRequest =
        HelmInstallCommandRequest.builder().gitConfig(new GitConfig()).build();
    assertThat(installCommandRequest.fetchRequiredExecutionCapabilities()
                   .stream()
                   .map(ExecutionCapability::getCapabilityType)
                   .collect(Collectors.toList()))
        .containsExactly(CapabilityType.HELM_COMMAND, CapabilityType.GIT_CONNECTION);
  }
}
