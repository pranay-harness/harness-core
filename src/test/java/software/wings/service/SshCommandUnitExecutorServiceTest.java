package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Artifact.Builder.anArtifact;
import static software.wings.beans.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.BastionConnectionAttributes.BastionConnectionAttributesBuilder.aBastionConnectionAttributes;
import static software.wings.beans.Host.Builder.aHost;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.HostConnectionAttributesBuilder.aHostConnectionAttributes;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.ScpCommandUnit.Builder.aScpCommandUnit;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.BASTION_HOST;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.KEY_AUTH;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.PASSWORD_AUTH;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.FILE_ID;
import static software.wings.utils.WingsTestConstants.FILE_PATH;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_ID;
import static software.wings.utils.WingsTestConstants.SSH_KEY;
import static software.wings.utils.WingsTestConstants.SSH_USER_NAME;
import static software.wings.utils.WingsTestConstants.SSH_USER_PASSWORD;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Host;
import software.wings.beans.Host.Builder;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit.ExecutionResult;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.InitCommandUnit;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshJumpboxExecutor;
import software.wings.core.ssh.executors.SshPubKeyAuthExecutor;
import software.wings.core.ssh.executors.SshPwdAuthExecutor;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.LogService;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.inject.Inject;

/**
 * Created by anubhaw on 5/31/16.
 */
public class SshCommandUnitExecutorServiceTest extends WingsBaseTest {
  /**
   * The constant EXEC_CMD.
   */
  public static final String EXEC_CMD = "ls";
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

  private static final ExecCommandUnit EXEC_COMMAND_UNIT = anExecCommandUnit().withCommandString(EXEC_CMD).build();
  /**
   * The Ssh executor factory.
   */
  @Mock SshExecutorFactory sshExecutorFactory;
  /**
   * The Ssh pwd auth executor.
   */
  @Mock SshPwdAuthExecutor sshPwdAuthExecutor;
  /**
   * The Ssh pub key auth executor.
   */
  @Mock SshPubKeyAuthExecutor sshPubKeyAuthExecutor;
  /**
   * The Ssh jumpbox executor.
   */
  @Mock SshJumpboxExecutor sshJumpboxExecutor;
  /**
   * The Log service.
   */
  @Mock LogService logService;

  @Inject @InjectMocks private CommandUnitExecutorService sshCommandUnitExecutorService;
  private Builder builder = aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withHostName(HOST_NAME);
  private CommandExecutionContext commandExecutionContext =
      aCommandExecutionContext()
          .withActivityId(ACTIVITY_ID)
          .withRuntimePath("/tmp/runtime")
          .withBackupPath("/tmp/backup")
          .withStagingPath("/tmp/staging")
          .withExecutionCredential(
              aSSHExecutionCredential().withSshUser(SSH_USER_NAME).withSshPassword(SSH_USER_PASSWORD).build())
          .withArtifact(anArtifact()
                            .withArtifactFiles(Lists.newArrayList(anArtifactFile().withFileUuid(FILE_ID).build()))
                            .build())
          .build();

  /**
   * Should create password based ssh config.
   */
  @Test
  public void shouldCreatePasswordBasedSshConfig() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD).build();
    SshSessionConfig expectedSshConfig = aSshSessionConfig()
                                             .withAppId(APP_ID)
                                             .withExecutionId(ACTIVITY_ID)
                                             .withHost(HOST_NAME)
                                             .withUserName(SSH_USER_NAME)
                                             .withExecutorType(PASSWORD_AUTH)
                                             .withPassword(SSH_USER_PASSWORD)
                                             .build();

    when(sshExecutorFactory.getExecutor(PASSWORD_AUTH)).thenReturn(sshPwdAuthExecutor);
    sshCommandUnitExecutorService.execute(host, EXEC_COMMAND_UNIT, commandExecutionContext);
    verify(sshExecutorFactory).getExecutor(PASSWORD_AUTH);
    verify(sshPwdAuthExecutor).init(expectedSshConfig);
  }

  /**
   * Should create key based ssh config.
   */
  @Test
  public void shouldCreateKeyBasedSshConfig() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_KEY).build();
    SshSessionConfig expectedSshConfig = aSshSessionConfig()
                                             .withAppId(APP_ID)
                                             .withExecutionId(ACTIVITY_ID)
                                             .withHost(HOST_NAME)
                                             .withUserName(SSH_USER_NAME)
                                             .withExecutorType(KEY_AUTH)
                                             .withKey(SSH_KEY)
                                             .build();

    when(sshExecutorFactory.getExecutor(KEY_AUTH)).thenReturn(sshPubKeyAuthExecutor);
    sshCommandUnitExecutorService.execute(host, EXEC_COMMAND_UNIT, commandExecutionContext);
    verify(sshExecutorFactory).getExecutor(KEY_AUTH);
    verify(sshPubKeyAuthExecutor).init(expectedSshConfig);
  }

  /**
   * Should create bastion host based ssh config.
   */
  @Test
  public void shouldCreateBastionHostBasedSshConfig() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD).withBastionConnAttr(BASTION_HOST_ATTR).build();
    SshSessionConfig expectedSshConfig =
        aSshSessionConfig()
            .withAppId(APP_ID)
            .withExecutionId(ACTIVITY_ID)
            .withHost(HOST_NAME)
            .withUserName(SSH_USER_NAME)
            .withExecutorType(BASTION_HOST)
            .withPassword(SSH_USER_PASSWORD)
            .withBastionHostConfig(aSshSessionConfig().withHost(HOST_NAME).withKey(SSH_KEY).build())
            .build();

    when(sshExecutorFactory.getExecutor(BASTION_HOST)).thenReturn(sshJumpboxExecutor);
    sshCommandUnitExecutorService.execute(host, EXEC_COMMAND_UNIT, commandExecutionContext);
    verify(sshExecutorFactory).getExecutor(BASTION_HOST);
    verify(sshJumpboxExecutor).init(expectedSshConfig);
  }

  /**
   * Should execute exec command.
   */
  @Test
  public void shouldExecuteExecCommand() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD).build();
    when(sshExecutorFactory.getExecutor(PASSWORD_AUTH)).thenReturn(sshPwdAuthExecutor);
    sshCommandUnitExecutorService.execute(host, EXEC_COMMAND_UNIT, commandExecutionContext);
    verify(sshPwdAuthExecutor).executeCommandString(EXEC_COMMAND_UNIT.getPreparedCommand());
  }

  /**
   * Should execute copy command.
   */
  @Test
  public void shouldExecuteCopyCommand() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD).build();
    ScpCommandUnit commandUnit = aScpCommandUnit()
                                     .withCommandUnitType(CommandUnitType.SCP)
                                     .withFileBucket(ARTIFACTS)
                                     .withDestinationDirectoryPath(FILE_PATH)
                                     .withFileCategory(ScpFileCategory.ARTIFACTS)
                                     .build();

    when(sshExecutorFactory.getExecutor(PASSWORD_AUTH)).thenReturn(sshPwdAuthExecutor);
    sshCommandUnitExecutorService.execute(host, commandUnit, commandExecutionContext);
    verify(sshPwdAuthExecutor)
        .copyGridFsFiles(
            commandUnit.getDestinationDirectoryPath(), commandUnit.getFileBucket(), commandUnit.getFileIds());
  }

  /**
   * Should execute init command.
   *
   * @throws IOException the io exception
   */
  @Test
  public void shouldExecuteInitCommand() throws IOException {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD).build();
    InitCommandUnit commandUnit = new InitCommandUnit();
    Command command =
        aCommand()
            .withCommandUnits(asList(commandUnit,
                anExecCommandUnit().withName("dols").withCommandPath("/tmp").withCommandString("ls").build()))
            .build();
    commandUnit.setCommand(command);

    when(sshExecutorFactory.getExecutor(PASSWORD_AUTH)).thenReturn(sshPwdAuthExecutor);
    when(sshPwdAuthExecutor.executeCommandString(anyString())).thenReturn(ExecutionResult.SUCCESS);
    when(sshPwdAuthExecutor.copyFiles(anyString(), anyListOf(String.class))).thenReturn(ExecutionResult.SUCCESS);

    sshCommandUnitExecutorService.execute(host, commandUnit, commandExecutionContext);
    verify(sshPwdAuthExecutor).executeCommandString("mkdir -p /tmp/ACTIVITY_ID");

    String actualLauncherScript =
        new File(System.getProperty("java.io.tmpdir"), "wingslauncherACTIVITY_ID.sh").getAbsolutePath();
    verify(sshPwdAuthExecutor).copyFiles("/tmp/ACTIVITY_ID", asList(actualLauncherScript));
    assertThat(new File(actualLauncherScript))
        .hasContent(
            CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedLaunchScript.sh"))));

    String expectedExecCommandUnitScript =
        new File(System.getProperty("java.io.tmpdir"), "wings" + DigestUtils.md5Hex("dolsACTIVITY_ID"))
            .getAbsolutePath();
    verify(sshPwdAuthExecutor).copyFiles("/tmp/ACTIVITY_ID", asList(expectedExecCommandUnitScript));
    verify(sshPwdAuthExecutor).executeCommandString("chmod 0744 /tmp/ACTIVITY_ID/*");

    assertThat(new File(expectedExecCommandUnitScript)).hasContent("ls");
    assertThat((ExecCommandUnit) command.getCommandUnits().get(1))
        .extracting(ExecCommandUnit::getPreparedCommand)
        .contains(
            "/tmp/ACTIVITY_ID/wingslauncherACTIVITY_ID.sh -w '/tmp' wings" + DigestUtils.md5Hex("dolsACTIVITY_ID"));
  }

  /**
   * Should execute init command with nested units.
   *
   * @throws IOException the io exception
   */
  @Test
  public void shouldExecuteInitCommandWithNestedUnits() throws IOException {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD).build();
    InitCommandUnit commandUnit = new InitCommandUnit();
    Command command =
        aCommand()
            .withCommandUnits(asList(commandUnit,
                anExecCommandUnit().withName("dols").withCommandPath("/tmp").withCommandString("ls").build(),
                aCommand()
                    .withName("start1")
                    .withCommandUnits(asList(anExecCommandUnit()
                                                 .withName("startscript")
                                                 .withCommandString("start.sh")
                                                 .withCommandPath("/home/tomcat")
                                                 .build()))
                    .build()))
            .build();
    commandUnit.setCommand(command);

    when(sshExecutorFactory.getExecutor(PASSWORD_AUTH)).thenReturn(sshPwdAuthExecutor);
    when(sshPwdAuthExecutor.executeCommandString(anyString())).thenReturn(ExecutionResult.SUCCESS);
    when(sshPwdAuthExecutor.copyFiles(anyString(), anyListOf(String.class))).thenReturn(ExecutionResult.SUCCESS);
    sshCommandUnitExecutorService.execute(host, commandUnit, commandExecutionContext);

    String expectedExecCommandUnitScript =
        new File(System.getProperty("java.io.tmpdir"), "wings" + DigestUtils.md5Hex("dolsACTIVITY_ID"))
            .getAbsolutePath();
    String expectedSubExecCommandUnitScript =
        new File(System.getProperty("java.io.tmpdir"), "wings" + DigestUtils.md5Hex("start1startscriptACTIVITY_ID"))
            .getAbsolutePath();

    verify(sshPwdAuthExecutor)
        .copyFiles("/tmp/ACTIVITY_ID", asList(expectedExecCommandUnitScript, expectedSubExecCommandUnitScript));

    assertThat(new File(expectedExecCommandUnitScript)).hasContent("ls");
    assertThat((ExecCommandUnit) command.getCommandUnits().get(1))
        .extracting(ExecCommandUnit::getPreparedCommand)
        .contains(
            "/tmp/ACTIVITY_ID/wingslauncherACTIVITY_ID.sh -w '/tmp' wings" + DigestUtils.md5Hex("dolsACTIVITY_ID"));

    assertThat(new File(expectedSubExecCommandUnitScript)).hasContent("start.sh");
    assertThat((ExecCommandUnit) ((Command) command.getCommandUnits().get(2)).getCommandUnits().get(0))
        .extracting(ExecCommandUnit::getPreparedCommand)
        .contains("/tmp/ACTIVITY_ID/wingslauncherACTIVITY_ID.sh -w '/home/tomcat' wings"
            + DigestUtils.md5Hex("start1startscriptACTIVITY_ID"));

    verify(sshPwdAuthExecutor).executeCommandString("chmod 0744 /tmp/ACTIVITY_ID/*");
  }
}
