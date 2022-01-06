/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ANSHUL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.configuration.InstallUtils;
import io.harness.k8s.model.HelmVersion;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

@OwnedBy(CDP)
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
