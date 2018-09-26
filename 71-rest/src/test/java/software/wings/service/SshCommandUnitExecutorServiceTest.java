package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.BastionConnectionAttributes.Builder.aBastionConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.artifact.ArtifactStreamAttributes.Builder.anArtifactStreamAttributes;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.ScpCommandUnit.Builder.aScpCommandUnit;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.BASTION_HOST;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.KEY_AUTH;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.PASSWORD_AUTH;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.BASTION_CONN_ATTR_ID;
import static software.wings.utils.WingsTestConstants.FILE_ID;
import static software.wings.utils.WingsTestConstants.FILE_PATH;
import static software.wings.utils.WingsTestConstants.HOST_CONN_ATTR_ID;
import static software.wings.utils.WingsTestConstants.HOST_CONN_ATTR_KEY_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.SSH_KEY;
import static software.wings.utils.WingsTestConstants.SSH_USER_NAME;
import static software.wings.utils.WingsTestConstants.SSH_USER_PASSWORD;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.FakeTimeLimiter;
import com.google.inject.Injector;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.InitSshCommandUnit;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.Host.Builder;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshJumpboxExecutor;
import software.wings.core.ssh.executors.SshPubKeyAuthExecutor;
import software.wings.core.ssh.executors.SshPwdAuthExecutor;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.SshCommandUnitExecutorServiceImpl;
import software.wings.service.intfc.CommandUnitExecutorService;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by anubhaw on 5/31/16.
 */
public class SshCommandUnitExecutorServiceTest extends WingsBaseTest {
  /**
   * The constant EXEC_CMD.
   */
  public static final String EXEC_CMD = "ls";
  private static final SettingAttribute HOST_CONN_ATTR_PWD =
      aSettingAttribute()
          .withUuid(HOST_CONN_ATTR_ID)
          .withValue(aHostConnectionAttributes().withAccessType(USER_PASSWORD).build())
          .build();
  private static final SettingAttribute BASTION_HOST_ATTR =
      aSettingAttribute()
          .withUuid(BASTION_CONN_ATTR_ID)
          .withValue(aBastionConnectionAttributes().withHostName(HOST_NAME).withKey(SSH_KEY).build())
          .build();
  private static final SettingAttribute HOST_CONN_ATTR_KEY = aSettingAttribute()
                                                                 .withUuid(HOST_CONN_ATTR_KEY_ID)
                                                                 .withValue(aHostConnectionAttributes()
                                                                                .withAccessType(AccessType.KEY)
                                                                                .withKey(SSH_KEY)
                                                                                .withUserName(SSH_USER_NAME)
                                                                                .build())
                                                                 .build();

  private static final ExecCommandUnit EXEC_COMMAND_UNIT = anExecCommandUnit().withCommandString(EXEC_CMD).build();
  /**
   * The Ssh executor factory.
   */
  @Mock private SshExecutorFactory sshExecutorFactory;
  /**
   * The Ssh pwd auth executor.
   */
  @Mock private SshPwdAuthExecutor sshPwdAuthExecutor;
  /**
   * The Ssh pub key auth executor.
   */
  @Mock private SshPubKeyAuthExecutor sshPubKeyAuthExecutor;
  /**
   * The Ssh jumpbox executor.
   */
  @Mock private SshJumpboxExecutor sshJumpboxExecutor;

  @Mock private Injector injector;
  /**
   * The Log service.
   */
  @Mock private DelegateLogService logService;

  @InjectMocks
  private CommandUnitExecutorService sshCommandUnitExecutorService = new SshCommandUnitExecutorServiceImpl();

  private Builder builder = aHost().withAppId(APP_ID).withHostName(HOST_NAME);
  private CommandExecutionContext.Builder commandExecutionContextBuider =
      aCommandExecutionContext()
          .withAppId(APP_ID)
          .withActivityId(ACTIVITY_ID)
          .withRuntimePath("/tmp/runtime")
          .withBackupPath("/tmp/backup")
          .withStagingPath("/tmp/staging")
          .withExecutionCredential(
              aSSHExecutionCredential().withSshUser(SSH_USER_NAME).withSshPassword(SSH_USER_PASSWORD).build())
          .withArtifactFiles(
              Lists.newArrayList(anArtifactFile().withName("artifact.war").withFileUuid(FILE_ID).build()))
          .withServiceVariables(ImmutableMap.of("PORT", "8080", "PASSWORD", "aSecret"))
          .withSafeDisplayServiceVariables(ImmutableMap.of("PORT", "8080", "PASSWORD", "*****"))
          .withAccountId(ACCOUNT_ID);

  @Before
  public void setupMocks() {
    on(sshCommandUnitExecutorService).set("timeLimiter", new FakeTimeLimiter());
    /*when(settingsService.get(HOST_CONN_ATTR_ID)).thenReturn(HOST_CONN_ATTR_PWD);
    when(settingsService.get(BASTION_CONN_ATTR_ID)).thenReturn(BASTION_HOST_ATTR);
    when(settingsService.get(HOST_CONN_ATTR_KEY_ID)).thenReturn(HOST_CONN_ATTR_KEY);*/
  }

  /**
   * Should create password based ssh config.
   */
  @Test
  public void shouldCreatePasswordBasedSshConfig() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid()).build();
    SshSessionConfig expectedSshConfig = aSshSessionConfig()
                                             .withAppId(APP_ID)
                                             .withExecutionId(ACTIVITY_ID)
                                             .withHost(HOST_NAME)
                                             .withUserName(SSH_USER_NAME)
                                             .withExecutorType(PASSWORD_AUTH)
                                             .withPassword(SSH_USER_PASSWORD)
                                             .withAccountId(ACCOUNT_ID)
                                             .build();

    when(sshExecutorFactory.getExecutor(PASSWORD_AUTH)).thenReturn(sshPwdAuthExecutor);
    sshCommandUnitExecutorService.execute(host, EXEC_COMMAND_UNIT,
        commandExecutionContextBuider.but().withHostConnectionAttributes(HOST_CONN_ATTR_PWD).build());
    verify(sshExecutorFactory).getExecutor(PASSWORD_AUTH);
    verify(sshPwdAuthExecutor).init(expectedSshConfig);
  }

  /**
   * Should create key based ssh config.
   */
  @Test
  public void shouldCreateKeyBasedSshConfig() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_KEY.getUuid()).build();
    SshSessionConfig expectedSshConfig = aSshSessionConfig()
                                             .withAppId(APP_ID)
                                             .withExecutionId(ACTIVITY_ID)
                                             .withHost(HOST_NAME)
                                             .withUserName(SSH_USER_NAME)
                                             .withExecutorType(KEY_AUTH)
                                             .withKey(SSH_KEY)
                                             .withKeyName(HOST_CONN_ATTR_KEY.getUuid())
                                             .withAccountId(ACCOUNT_ID)
                                             .build();

    when(sshExecutorFactory.getExecutor(KEY_AUTH)).thenReturn(sshPubKeyAuthExecutor);
    sshCommandUnitExecutorService.execute(host, EXEC_COMMAND_UNIT,
        commandExecutionContextBuider.but().withHostConnectionAttributes(HOST_CONN_ATTR_KEY).build());
    verify(sshExecutorFactory).getExecutor(KEY_AUTH);
    verify(sshPubKeyAuthExecutor).init(expectedSshConfig);
  }

  /**
   * Should create bastion host based ssh config.
   */
  @Test
  public void shouldCreateBastionHostBasedSshConfig() {
    Host host =
        builder.withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid()).withBastionConnAttr(BASTION_HOST_ATTR.getUuid()).build();
    SshSessionConfig expectedSshConfig = aSshSessionConfig()
                                             .withAppId(APP_ID)
                                             .withExecutionId(ACTIVITY_ID)
                                             .withHost(HOST_NAME)
                                             .withUserName(SSH_USER_NAME)
                                             .withExecutorType(BASTION_HOST)
                                             .withPassword(SSH_USER_PASSWORD)
                                             .withBastionHostConfig(aSshSessionConfig()
                                                                        .withHost(HOST_NAME)
                                                                        .withKey(SSH_KEY)
                                                                        .withKeyName(BASTION_HOST_ATTR.getUuid())
                                                                        .build())
                                             .withAccountId(ACCOUNT_ID)
                                             .build();

    when(sshExecutorFactory.getExecutor(BASTION_HOST)).thenReturn(sshJumpboxExecutor);
    sshCommandUnitExecutorService.execute(host, EXEC_COMMAND_UNIT,
        commandExecutionContextBuider.but()
            .withHostConnectionAttributes(HOST_CONN_ATTR_PWD)
            .withBastionConnectionAttributes(BASTION_HOST_ATTR)
            .build());
    verify(sshExecutorFactory).getExecutor(BASTION_HOST);
    verify(sshJumpboxExecutor).init(expectedSshConfig);
  }

  /**
   * Should execute exec command.
   */
  @Test
  public void shouldExecuteExecCommand() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid()).build();
    when(sshExecutorFactory.getExecutor(PASSWORD_AUTH)).thenReturn(sshPwdAuthExecutor);
    sshCommandUnitExecutorService.execute(host, EXEC_COMMAND_UNIT,
        commandExecutionContextBuider.but().withHostConnectionAttributes(HOST_CONN_ATTR_PWD).build());
    verify(sshPwdAuthExecutor).executeCommandString(EXEC_COMMAND_UNIT.getPreparedCommand(), true);
  }

  /**
   * Should execute copy command.
   */
  @Test
  public void shouldExecuteCopyCommand() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid()).build();
    ScpCommandUnit commandUnit = aScpCommandUnit()
                                     .withCommandUnitType(CommandUnitType.SCP)
                                     .withDestinationDirectoryPath(FILE_PATH)
                                     .withFileCategory(ScpFileCategory.ARTIFACTS)
                                     .build();

    when(sshExecutorFactory.getExecutor(PASSWORD_AUTH)).thenReturn(sshPwdAuthExecutor);
    ArtifactStreamAttributes artifactStreamAttributes = anArtifactStreamAttributes().withMetadataOnly(false).build();
    sshCommandUnitExecutorService.execute(host, commandUnit,
        commandExecutionContextBuider.but()
            .withHostConnectionAttributes(HOST_CONN_ATTR_PWD)
            .withArtifactStreamAttributes(artifactStreamAttributes)
            .build());
    verify(sshPwdAuthExecutor)
        .copyGridFsFiles(commandUnit.getDestinationDirectoryPath(), ARTIFACTS,
            Lists.newArrayList(org.apache.commons.lang3.tuple.Pair.of(FILE_ID, null)));
  }

  /**
   * Should execute init command.
   *
   * @throws IOException the io exception
   */
  @Test
  public void shouldExecuteInitCommand() throws IOException {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid()).build();
    InitSshCommandUnit commandUnit = new InitSshCommandUnit();
    Command command =
        aCommand()
            .withCommandUnits(asList(commandUnit,
                anExecCommandUnit().withName("dols").withCommandPath("/tmp").withCommandString("ls").build()))
            .build();
    commandUnit.setCommand(command);

    when(sshExecutorFactory.getExecutor(PASSWORD_AUTH)).thenReturn(sshPwdAuthExecutor);
    when(sshPwdAuthExecutor.executeCommandString(anyString(), anyBoolean())).thenReturn(CommandExecutionStatus.SUCCESS);
    when(sshPwdAuthExecutor.copyFiles(anyString(), anyListOf(String.class))).thenReturn(CommandExecutionStatus.SUCCESS);

    sshCommandUnitExecutorService.execute(host, commandUnit,
        commandExecutionContextBuider.but().withHostConnectionAttributes(HOST_CONN_ATTR_PWD).build());
    verify(sshPwdAuthExecutor).executeCommandString("mkdir -p /tmp/ACTIVITY_ID", true);

    String actualLauncherScript =
        new File(System.getProperty("java.io.tmpdir"), "harnesslauncherACTIVITY_ID.sh").getAbsolutePath();
    verify(sshPwdAuthExecutor).copyFiles("/tmp/ACTIVITY_ID", asList(actualLauncherScript));
    assertThat(new File(actualLauncherScript))
        .hasContent(
            CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedLaunchScript.sh"))));

    String expectedExecCommandUnitScript =
        new File(System.getProperty("java.io.tmpdir"), "harness" + DigestUtils.md5Hex("dolsACTIVITY_ID"))
            .getAbsolutePath();
    verify(sshPwdAuthExecutor).copyFiles("/tmp/ACTIVITY_ID", asList(expectedExecCommandUnitScript));
    verify(sshPwdAuthExecutor).executeCommandString("chmod 0744 /tmp/ACTIVITY_ID/*", true);

    assertThat(new File(expectedExecCommandUnitScript)).hasContent("ls");
    assertThat((ExecCommandUnit) command.getCommandUnits().get(1))
        .extracting(ExecCommandUnit::getPreparedCommand)
        .contains(
            "/tmp/ACTIVITY_ID/harnesslauncherACTIVITY_ID.sh -w '/tmp' harness" + DigestUtils.md5Hex("dolsACTIVITY_ID"));
  }

  /**
   * Should execute init command with nested units.
   *
   * @throws IOException the io exception
   */
  @Test
  public void shouldExecuteInitCommandWithNestedUnits() throws IOException {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid()).build();
    InitSshCommandUnit commandUnit = new InitSshCommandUnit();
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
    when(sshPwdAuthExecutor.executeCommandString(anyString(), anyBoolean())).thenReturn(CommandExecutionStatus.SUCCESS);
    when(sshPwdAuthExecutor.copyFiles(anyString(), anyListOf(String.class))).thenReturn(CommandExecutionStatus.SUCCESS);
    sshCommandUnitExecutorService.execute(host, commandUnit,
        commandExecutionContextBuider.but().withHostConnectionAttributes(HOST_CONN_ATTR_PWD).build());

    String expectedExecCommandUnitScript =
        new File(System.getProperty("java.io.tmpdir"), "harness" + DigestUtils.md5Hex("dolsACTIVITY_ID"))
            .getAbsolutePath();
    String expectedSubExecCommandUnitScript =
        new File(System.getProperty("java.io.tmpdir"), "harness" + DigestUtils.md5Hex("start1startscriptACTIVITY_ID"))
            .getAbsolutePath();

    verify(sshPwdAuthExecutor)
        .copyFiles("/tmp/ACTIVITY_ID", asList(expectedExecCommandUnitScript, expectedSubExecCommandUnitScript));

    assertThat(new File(expectedExecCommandUnitScript)).hasContent("ls");
    assertThat((ExecCommandUnit) command.getCommandUnits().get(1))
        .extracting(ExecCommandUnit::getPreparedCommand)
        .contains(
            "/tmp/ACTIVITY_ID/harnesslauncherACTIVITY_ID.sh -w '/tmp' harness" + DigestUtils.md5Hex("dolsACTIVITY_ID"));

    assertThat(new File(expectedSubExecCommandUnitScript)).hasContent("start.sh");
    assertThat((ExecCommandUnit) ((Command) command.getCommandUnits().get(2)).getCommandUnits().get(0))
        .extracting(ExecCommandUnit::getPreparedCommand)
        .contains("/tmp/ACTIVITY_ID/harnesslauncherACTIVITY_ID.sh -w '/home/tomcat' harness"
            + DigestUtils.md5Hex("start1startscriptACTIVITY_ID"));

    verify(sshPwdAuthExecutor).executeCommandString("chmod 0744 /tmp/ACTIVITY_ID/*", true);
  }
}
