package software.wings.delegatetasks.validation.capabilitycheck;

import static io.harness.delegate.task.shell.ScriptType.BASH;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.sm.states.ShellScriptState.ConnectionType.SSH;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.jcraft.jsch.JSchException;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.delegatetasks.validation.capabilities.ShellConnectionCapability;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.WingsTestConstants;

public class ShellConnectionCapabilityCheckTest extends WingsBaseTest {
  @Mock private EncryptionService encryptionService;
  @Spy @InjectMocks ShellConnectionCapabilityCheck shellConnectionCapabilityCheck;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void performCapabilityCheck() throws JSchException {
    doNothing().when(shellConnectionCapabilityCheck).performTest(any(SshSessionConfig.class));
    CapabilityResponse capabilityResponse = shellConnectionCapabilityCheck.performCapabilityCheck(
        ShellConnectionCapability.builder()
            .shellScriptParameters(
                ShellScriptParameters.builder()
                    .accountId(ACCOUNT_ID)
                    .appId(APP_ID)
                    .activityId(ACTIVITY_ID)
                    .executeOnDelegate(false)
                    .connectionType(SSH)
                    .scriptType(BASH)
                    .hostConnectionAttributes(aHostConnectionAttributes()
                                                  .withAccessType(HostConnectionAttributes.AccessType.USER_PASSWORD)
                                                  .withAccountId(WingsTestConstants.ACCOUNT_ID)
                                                  .build())
                    .build())
            .build());
    assertThat(capabilityResponse).isNotNull();
    assertThat(capabilityResponse.isValidated()).isTrue();
  }
}
