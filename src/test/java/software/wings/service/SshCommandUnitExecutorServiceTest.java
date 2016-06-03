package software.wings.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.BastionConnectionAttributes.BastionConnectionAttributesBuilder.aBastionConnectionAttributes;
import static software.wings.beans.CommandUnitType.COPY_ARTIFACT;
import static software.wings.beans.CopyArtifactCommandUnit.Builder.aCopyArtifactCommandUnit;
import static software.wings.beans.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.Host.HostBuilder.aHost;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.HostConnectionAttributesBuilder.aHostConnectionAttributes;
import static software.wings.beans.HostConnectionCredential.HostConnectionCredentialBuilder.aHostConnectionCredential;
import static software.wings.beans.SettingAttribute.SettingAttributeBuilder.aSettingAttribute;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.BASTION_HOST;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.KEY_AUTH;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.PASSWORD_AUTH;
import static software.wings.core.ssh.executors.SshSessionConfig.SshSessionConfigBuilder.aSshSessionConfig;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;
import static software.wings.utils.WingsUnitTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsUnitTestConstants.FILE_ID;
import static software.wings.utils.WingsUnitTestConstants.FILE_PATH;
import static software.wings.utils.WingsUnitTestConstants.HOST_NAME;
import static software.wings.utils.WingsUnitTestConstants.SSH_KEY;
import static software.wings.utils.WingsUnitTestConstants.SSH_USER_NAME;
import static software.wings.utils.WingsUnitTestConstants.SSH_USER_PASSWORD;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.CopyCommandUnit;
import software.wings.beans.ExecCommandUnit;
import software.wings.beans.Host;
import software.wings.beans.Host.HostBuilder;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.HostConnectionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshJumpboxExecutor;
import software.wings.core.ssh.executors.SshPubKeyAuthExecutor;
import software.wings.core.ssh.executors.SshPwdAuthExecutor;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.service.intfc.CommandUnitExecutorService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 5/31/16.
 */

public class SshCommandUnitExecutorServiceTest extends WingsBaseTest {
  public static final String EXEC_CMD = "ls";
  @Mock SshExecutorFactory sshExecutorFactory;
  @Mock SshPwdAuthExecutor sshPwdAuthExecutor;
  @Mock SshPubKeyAuthExecutor sshPubKeyAuthExecutor;
  @Mock SshJumpboxExecutor sshJumpboxExecutor;

  @Inject @InjectMocks private CommandUnitExecutorService sshCommandUnitExecutorService;

  private static final SettingAttribute HOST_CONN_ATTR_PWD =
      aSettingAttribute().withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build()).build();

  private static final SettingAttribute BASTION_HOST_ATTR =
      aSettingAttribute()
          .withValue(aBastionConnectionAttributes().withHostName(HOST_NAME).withKey(SSH_KEY).build())
          .build();

  private static final SettingAttribute HOST_CONN_ATTR_KEY =
      aSettingAttribute()
          .withValue(aHostConnectionAttributes().withAccessType(AccessType.KEY).withKey(SSH_KEY).build())
          .build();

  private static final HostConnectionCredential CREDENTIAL =
      aHostConnectionCredential().withSshUser(SSH_USER_NAME).withSshPassword(SSH_USER_PASSWORD).build();

  private HostBuilder hostBuilder = aHost().withHostName(HOST_NAME);
  private static final ExecCommandUnit EXEC_COMMAND_UNIT = anExecCommandUnit().withCommandString(EXEC_CMD).build();

  @Test
  public void shouldCreatePasswordBasedSshConfig() {
    Host host = hostBuilder.withHostConnAttr(HOST_CONN_ATTR_PWD).withHostConnectionCredential(CREDENTIAL).build();
    SshSessionConfig expectedSshConfig = aSshSessionConfig()
                                             .withExecutionId(ACTIVITY_ID)
                                             .withHost(HOST_NAME)
                                             .withUserName(SSH_USER_NAME)
                                             .withExecutorType(PASSWORD_AUTH)
                                             .withPassword(SSH_USER_PASSWORD)
                                             .build();

    when(sshExecutorFactory.getExecutor(PASSWORD_AUTH)).thenReturn(sshPwdAuthExecutor);
    sshCommandUnitExecutorService.execute(host, EXEC_COMMAND_UNIT, ACTIVITY_ID);
    verify(sshExecutorFactory).getExecutor(PASSWORD_AUTH);
    verify(sshPwdAuthExecutor).init(expectedSshConfig);
  }

  @Test
  public void shouldCreateKeyBasedSshConfig() {
    Host host = hostBuilder.withHostConnAttr(HOST_CONN_ATTR_KEY)
                    .withHostConnectionCredential(aHostConnectionCredential().withSshUser(SSH_USER_NAME).build())
                    .build();
    SshSessionConfig expectedSshConfig = aSshSessionConfig()
                                             .withExecutionId(ACTIVITY_ID)
                                             .withHost(HOST_NAME)
                                             .withUserName(SSH_USER_NAME)
                                             .withExecutorType(KEY_AUTH)
                                             .withKey(SSH_KEY)
                                             .build();

    when(sshExecutorFactory.getExecutor(KEY_AUTH)).thenReturn(sshPubKeyAuthExecutor);
    sshCommandUnitExecutorService.execute(host, EXEC_COMMAND_UNIT, ACTIVITY_ID);
    verify(sshExecutorFactory).getExecutor(KEY_AUTH);
    verify(sshPubKeyAuthExecutor).init(expectedSshConfig);
  }

  @Test
  public void shouldCreateBastionHostBasedSshConfig() {
    Host host = hostBuilder.withHostConnAttr(HOST_CONN_ATTR_PWD)
                    .withHostConnectionCredential(CREDENTIAL)
                    .withBastionConnAttr(BASTION_HOST_ATTR)
                    .build();
    SshSessionConfig expectedSshConfig =
        aSshSessionConfig()
            .withExecutionId(ACTIVITY_ID)
            .withHost(HOST_NAME)
            .withUserName(SSH_USER_NAME)
            .withExecutorType(BASTION_HOST)
            .withPassword(SSH_USER_PASSWORD)
            .withJumpboxConfig(aSshSessionConfig().withHost(HOST_NAME).withKey(SSH_KEY).build())
            .build();

    when(sshExecutorFactory.getExecutor(BASTION_HOST)).thenReturn(sshJumpboxExecutor);
    sshCommandUnitExecutorService.execute(host, EXEC_COMMAND_UNIT, ACTIVITY_ID);
    verify(sshExecutorFactory).getExecutor(BASTION_HOST);
    verify(sshJumpboxExecutor).init(expectedSshConfig);
  }

  @Test
  public void shouldExecuteExecCommand() {
    Host host = hostBuilder.withHostConnAttr(HOST_CONN_ATTR_PWD).withHostConnectionCredential(CREDENTIAL).build();

    when(sshExecutorFactory.getExecutor(PASSWORD_AUTH)).thenReturn(sshPwdAuthExecutor);
    sshCommandUnitExecutorService.execute(host, EXEC_COMMAND_UNIT, ACTIVITY_ID);
    verify(sshPwdAuthExecutor).execute(EXEC_CMD);
  }

  @Test
  public void shouldExecuteCopyCommand() {
    Host host = hostBuilder.withHostConnAttr(HOST_CONN_ATTR_PWD).withHostConnectionCredential(CREDENTIAL).build();
    CopyCommandUnit commandUnit = aCopyArtifactCommandUnit()
                                      .withCommandUnitType(COPY_ARTIFACT)
                                      .withFileId(FILE_ID)
                                      .withFileBucket(ARTIFACTS)
                                      .withDestinationFilePath(FILE_PATH)
                                      .build();

    when(sshExecutorFactory.getExecutor(PASSWORD_AUTH)).thenReturn(sshPwdAuthExecutor);
    sshCommandUnitExecutorService.execute(host, commandUnit, ACTIVITY_ID);
    verify(sshPwdAuthExecutor).transferFile(FILE_ID, FILE_PATH, ARTIFACTS);
  }
}
