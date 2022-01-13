/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.KustomizeCapability;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({KustomizeCapabilityCheck.class})
@OwnedBy(CDP)
public class KustomizeCapabilityCheckTest extends CategoryTest {
  @Mock private KustomizeCapability capability;
  @InjectMocks private KustomizeCapabilityCheck capabilityCheck;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void pluginsExist() {
    PowerMockito.mockStatic(KustomizeCapabilityCheck.class);
    when(KustomizeCapabilityCheck.doesKustomizePluginDirExist(any())).thenReturn(true);
    assertThat(capabilityCheck.performCapabilityCheck(capability))
        .isEqualTo(CapabilityResponse.builder().validated(true).delegateCapability(capability).build());
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void pluginsDoNotExist() {
    PowerMockito.mockStatic(KustomizeCapabilityCheck.class);
    when(KustomizeCapabilityCheck.doesKustomizePluginDirExist(any())).thenReturn(false);
    assertThat(capabilityCheck.performCapabilityCheck(capability))
        .isEqualTo(CapabilityResponse.builder().validated(false).delegateCapability(capability).build());
  }
}
