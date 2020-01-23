package io.harness.delegate.service;

import static io.harness.rule.OwnerRule.ANSHUL;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.configuration.InstallUtils;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class K8sGlobalConfigServiceImplTest extends CategoryTest {
  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void shouldNotApplyFunctorIfNoSecrets() {
    assertThat(InstallUtils.getOcPath()).isEqualTo("oc");
  }
}
