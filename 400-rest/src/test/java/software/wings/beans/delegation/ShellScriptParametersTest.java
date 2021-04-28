package software.wings.beans.delegation;

import static io.harness.annotations.dev.HarnessModule._950_DELEGATE_TASKS_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.PARDHA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.rule.Owner;

import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.ContainerServiceParams;

import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@TargetModule(_950_DELEGATE_TASKS_BEANS)
@OwnedBy(CDP)
public class ShellScriptParametersTest extends CategoryTest {
  @Mock ContainerServiceParams containerServiceParams;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = PARDHA)
  @Category(UnitTests.class)
  public void fetchRequiredExecutionCapabilities() {
    SettingAttribute settingAttribute = new SettingAttribute();
    settingAttribute.setValue(KubernetesClusterConfig.builder().useKubernetesDelegate(true).build());
    when(containerServiceParams.getSettingAttribute()).thenReturn(settingAttribute);
    when(containerServiceParams.fetchRequiredExecutionCapabilities(null))
        .thenReturn(Arrays.asList(SelectorCapability.builder().build()));
    ShellScriptParameters shellScriptParameters = ShellScriptParameters.builder()
                                                      .script("")
                                                      .executeOnDelegate(true)
                                                      .containerServiceParams(containerServiceParams)
                                                      .build();
    assertThat(shellScriptParameters.fetchRequiredExecutionCapabilities(null)).isEmpty();

    shellScriptParameters.setScript("HARNESS_KUBE_CONFIG_PATH");
    assertThat(shellScriptParameters.fetchRequiredExecutionCapabilities(null))
        .containsExactly(SelectorCapability.builder().build());
  }
}
