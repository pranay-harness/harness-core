package io.harness.delegate.service;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.configuration.InstallUtils;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;

public class K8sGlobalConfigServiceImplTest extends CategoryTest {
  private K8sGlobalConfigServiceImpl k8sGlobalConfigService = Mockito.spy(K8sGlobalConfigServiceImpl.class);

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getHelmPath() {
    assertThat(k8sGlobalConfigService.getHelmPath(null)).isNotNull();
    assertThat(k8sGlobalConfigService.getHelmPath(HelmVersion.V3)).isNotNull();
    assertThat(k8sGlobalConfigService.getHelmPath(HelmVersion.V2)).isNotNull();
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void shouldNotApplyFunctorIfNoSecrets() {
    assertThat(InstallUtils.getOcPath()).isEqualTo("oc");
  }
}
