/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.SmtpCapability;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SmtpCapabilityCheck.class})
public class SmtpCapabilityCheckTest extends CategoryTest {
  private SmtpCapability smtpCapability = SmtpCapability.builder().build();

  @Inject @InjectMocks SmtpCapabilityCheck smtpCapabilityCheck;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldPerformCapabilityCheck() {
    PowerMockito.mockStatic(SmtpCapabilityCheck.class);
    when(SmtpCapabilityCheck.isCapable(anyBoolean(), anyBoolean(), any(), anyInt(), any())).thenReturn(true);
    CapabilityResponse capabilityResponse = smtpCapabilityCheck.performCapabilityCheck(smtpCapability);
    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isTrue();
  }
}
