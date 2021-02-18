package io.harness.delegate.task.executioncapability;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.SSHHostValidationCapability;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.jcraft.jsch.JSchException;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SSHHostValidationCapabilityCheck.class})
public class SSHHostValidationCapabilityCheckTest extends CategoryTest {
  @Inject @InjectMocks private SSHHostValidationCapabilityCheck sshHostValidationCapabilityCheck;

  private final SSHHostValidationCapability validationCapability =
      SSHHostValidationCapability.builder().host("127.0.0.1").port(22).build();

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldPerformCapabilityCheck() throws JSchException {
    PowerMockito.mockStatic(SSHHostValidationCapabilityCheck.class);
    when(SSHHostValidationCapabilityCheck.checkConnectivity(any(), anyInt())).thenReturn(true);
    CapabilityResponse capabilityResponse =
        sshHostValidationCapabilityCheck.performCapabilityCheck(validationCapability);
    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isTrue();
  }
}
