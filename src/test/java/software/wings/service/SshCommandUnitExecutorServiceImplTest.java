package software.wings.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Host.HostBuilder.aHost;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.HostConnectionAttributesBuilder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionCredential.HostConnectionCredentialBuilder.aHostConnectionCredential;
import static software.wings.beans.SettingAttribute.SettingAttributeBuilder.aSettingAttribute;
import static software.wings.utils.EntityNameConstants.HOST_NAME;
import static software.wings.utils.EntityNameConstants.SSH_USER_NAME;
import static software.wings.utils.EntityNameConstants.SSH_USER_PASSWORD;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ExecCommandUnit;
import software.wings.beans.Host;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshJumpboxExecutor;
import software.wings.core.ssh.executors.SshPubKeyAuthExecutor;
import software.wings.core.ssh.executors.SshPwdAuthExecutor;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.ssh.executors.SshSudoExecutor;
import software.wings.service.impl.SshCommandUnitExecutorServiceImpl;

import javax.inject.Inject;

/**
 * Created by anubhaw on 5/31/16.
 */

public class SshCommandUnitExecutorServiceImplTest extends WingsBaseTest {
  public static final String EXEC_CMD = "ls";
  @Mock SshExecutorFactory sshExecutorFactory;
  @Mock SshPwdAuthExecutor sshPwdAuthExecutor;
  @Mock SshPubKeyAuthExecutor sshPubKeyAuthExecutor;
  @Mock SshJumpboxExecutor sshJumpboxExecutor;
  @Mock SshSudoExecutor sshSudoExecutor;

  @Inject @InjectMocks private SshCommandUnitExecutorServiceImpl sshCommandUnitExecutorService;

  @Test
  public void shouldExecuteExecCommand() {
    Host host =
        aHost()
            .withHostName(HOST_NAME)
            .withHostConnAttr(aSettingAttribute()
                                  .withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build())
                                  .build())
            .withHostConnectionCredential(
                aHostConnectionCredential().withSshUser(SSH_USER_NAME).withSshPassword(SSH_USER_PASSWORD).build())
            .build();
    ExecCommandUnit commandUnit = new ExecCommandUnit();
    commandUnit.setCommandString(EXEC_CMD);

    when(sshExecutorFactory.getExecutor(any(SshSessionConfig.class))).thenReturn(sshPwdAuthExecutor);
    sshCommandUnitExecutorService.execute(host, commandUnit);
    verify(sshPwdAuthExecutor).execute(EXEC_CMD);
  }
}
