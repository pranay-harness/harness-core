package io.harness.delegate.task.executioncapability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.KustomizeCapability;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.delegatetasks.validation.K8sValidationHelper;
import software.wings.helpers.ext.kustomize.KustomizeConfig;

public class KustomizeCapabilityCheckTest extends WingsBaseTest {
  @Mock private K8sValidationHelper validationHelper;
  @Mock private KustomizeCapability capability;
  @InjectMocks private KustomizeCapabilityCheck capabilityCheck;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void performCapabilityCheck() {
    pluginsExist();
    pluginsDoNotExist();
  }

  private void pluginsExist() {
    doReturn(true).when(validationHelper).doesKustomizePluginDirExist(any(KustomizeConfig.class));

    assertThat(capabilityCheck.performCapabilityCheck(capability))
        .isEqualTo(CapabilityResponse.builder().validated(true).delegateCapability(capability).build());
  }

  private void pluginsDoNotExist() {
    doReturn(false).when(validationHelper).doesKustomizePluginDirExist(any(KustomizeConfig.class));

    assertThat(capabilityCheck.performCapabilityCheck(capability))
        .isEqualTo(CapabilityResponse.builder().validated(false).delegateCapability(capability).build());
  }
}
