/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import static io.harness.rule.OwnerRule.IVAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.PcfInstallationCapability;
import io.harness.pcf.CfCliDelegateResolver;
import io.harness.pcf.model.CfCliVersion;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDP)
public class PcfInstallationCapabilityCheckTest extends CategoryTest {
  @Mock private CfCliDelegateResolver cfCliDelegateResolver;
  @InjectMocks private PcfInstallationCapabilityCheck pcfInstallationCapabilityCheck;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = IVAN)
  @Category(UnitTests.class)
  public void testPerformCapabilityCheck() {
    PcfInstallationCapability capability =
        PcfInstallationCapability.builder().criteria("CF CLI version 6 is installed").version(CfCliVersion.V6).build();
    doReturn(true).when(cfCliDelegateResolver).isDelegateEligibleToExecuteCfCliCommand(CfCliVersion.V6);

    CapabilityResponse capabilityResponse = pcfInstallationCapabilityCheck.performCapabilityCheck(capability);

    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isTrue();
    assertThat(capability.getVersion()).isEqualTo(CfCliVersion.V6);
    assertThat(capability.getCriteria()).isEqualTo("CF CLI version 6 is installed");
  }
}
