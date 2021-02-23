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
import io.harness.delegate.beans.executioncapability.WinrmHostValidationCapability;
import io.harness.rule.Owner;
import io.harness.winrm.WinRmChecker;

import com.jcraft.jsch.JSchException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({WinRmChecker.class})
public class WinrmHostValidationCapabilityCheckTest extends CategoryTest {
  @InjectMocks private WinrmHostValidationCapabilityCheck spyCapabilityCheck;

  private WinrmHostValidationCapability capability = WinrmHostValidationCapability.builder().build();

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldPerformCapabilityCheck() throws JSchException {
    PowerMockito.mockStatic(WinRmChecker.class);
    when(WinRmChecker.checkConnectivity(any(), anyInt(), anyBoolean(), any())).thenReturn(true);
    CapabilityResponse capabilityResponse = spyCapabilityCheck.performCapabilityCheck(capability);
    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isTrue();
  }
}
