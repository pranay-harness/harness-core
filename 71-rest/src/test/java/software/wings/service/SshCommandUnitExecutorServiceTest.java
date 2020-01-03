package software.wings.service;

import static io.harness.rule.OwnerRule.AADITI;
import static io.harness.rule.OwnerRule.ANUBHAW;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.BastionConnectionAttributes.Builder.aBastionConnectionAttributes;
import static software.wings.beans.HostConnectionAttributes.AccessType.USER_PASSWORD;
import static software.wings.beans.HostConnectionAttributes.AuthenticationScheme.SSH_KEY;
import static software.wings.beans.HostConnectionAttributes.Builder.aHostConnectionAttributes;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.ArtifactFile.Builder.anArtifactFile;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.command.ScpCommandUnit.Builder.aScpCommandUnit;
import static software.wings.beans.infrastructure.Host.Builder.aHost;
import static software.wings.core.ssh.executors.ScriptExecutor.ExecutorType.BASTION_HOST;
import static software.wings.core.ssh.executors.ScriptExecutor.ExecutorType.KEY_AUTH;
import static software.wings.core.ssh.executors.ScriptExecutor.ExecutorType.PASSWORD_AUTH;
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
import static software.wings.utils.WingsTestConstants.PUBLIC_DNS;
import static software.wings.utils.WingsTestConstants.SSH_USER_NAME;
import static software.wings.utils.WingsTestConstants.SSH_USER_PASSWORD;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.FakeTimeLimiter;
import com.google.inject.Injector;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.rule.Owner;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.HostConnectionAttributes.AccessType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnitHelper;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.InitSshCommandUnit;
import software.wings.beans.command.InitSshCommandUnitV2;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.beans.command.ScpCommandUnit.ScpFileCategory;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.Host.Builder;
import software.wings.core.ssh.executors.ScriptSshExecutor;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.service.impl.SshCommandUnitExecutorServiceImpl;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.utils.WingsTestConstants;

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
  private static final SettingAttribute HOST_CONN_ATTR_PWD = aSettingAttribute()
                                                                 .withUuid(HOST_CONN_ATTR_ID)
                                                                 .withValue(aHostConnectionAttributes()
                                                                                .withAccessType(USER_PASSWORD)
                                                                                .withUserName(SSH_USER_NAME)
                                                                                .withSshPassword(SSH_USER_PASSWORD)
                                                                                .build())
                                                                 .build();
  private static final SettingAttribute BASTION_HOST_ATTR =
      aSettingAttribute()
          .withUuid(BASTION_CONN_ATTR_ID)
          .withValue(aBastionConnectionAttributes().withHostName(HOST_NAME).withKey(WingsTestConstants.SSH_KEY).build())
          .build();
  private static final SettingAttribute HOST_CONN_ATTR_KEY = aSettingAttribute()
                                                                 .withUuid(HOST_CONN_ATTR_KEY_ID)
                                                                 .withValue(aHostConnectionAttributes()
                                                                                .withAccessType(AccessType.KEY)
                                                                                .withKey(WingsTestConstants.SSH_KEY)
                                                                                .withUserName(SSH_USER_NAME)
                                                                                .build())
                                                                 .build();

  private static final ExecCommandUnit EXEC_COMMAND_UNIT = anExecCommandUnit().withCommandString(EXEC_CMD).build();
  /**
   * The Ssh executor factory.
   */
  @Mock private SshExecutorFactory sshExecutorFactory;
  @Mock private ScriptSshExecutor scriptSshExecutor;

  @Mock private Injector injector;
  /**
   * The Log service.
   */
  @Mock private DelegateLogService logService;

  @InjectMocks
  private CommandUnitExecutorService sshCommandUnitExecutorService = new SshCommandUnitExecutorServiceImpl();

  private Builder builder = aHost().withAppId(APP_ID).withHostName(HOST_NAME).withPublicDns(PUBLIC_DNS);
  private CommandExecutionContext.Builder commandExecutionContextBuider =
      aCommandExecutionContext()
          .withAppId(APP_ID)
          .withActivityId(ACTIVITY_ID)
          .withRuntimePath("/tmp/runtime")
          .withBackupPath("/tmp/backup")
          .withStagingPath("/tmp/staging")
          .withExecutionCredential(aSSHExecutionCredential().withSshUser(SSH_USER_NAME).build())
          .withArtifactFiles(
              Lists.newArrayList(anArtifactFile().withName("artifact.war").withFileUuid(FILE_ID).build()))
          .withServiceVariables(ImmutableMap.of("PORT", "8080", "PASSWORD", "aSecret"))
          .withSafeDisplayServiceVariables(ImmutableMap.of("PORT", "8080", "PASSWORD", "*****"))
          .withHost(builder.build())
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
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldCreatePasswordBasedSshConfig() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid()).build();
    SshSessionConfig expectedSshConfig = aSshSessionConfig()
                                             .withAppId(APP_ID)
                                             .withExecutionId(ACTIVITY_ID)
                                             .withHost(PUBLIC_DNS)
                                             .withUserName(SSH_USER_NAME)
                                             .withExecutorType(PASSWORD_AUTH)
                                             .withSshPassword(SSH_USER_PASSWORD)
                                             .withAccountId(ACCOUNT_ID)
                                             .withAccessType(USER_PASSWORD)
                                             .withAuthenticationScheme(SSH_KEY)
                                             .build();

    when(sshExecutorFactory.getExecutor(expectedSshConfig)).thenReturn(scriptSshExecutor);
    sshCommandUnitExecutorService.execute(EXEC_COMMAND_UNIT,
        commandExecutionContextBuider.but().withHostConnectionAttributes(HOST_CONN_ATTR_PWD).build());
    verify(sshExecutorFactory).getExecutor(expectedSshConfig);
  }

  /**
   * Should create key based ssh config.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldCreateKeyBasedSshConfig() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_KEY.getUuid()).build();
    SshSessionConfig expectedSshConfig = aSshSessionConfig()
                                             .withAppId(APP_ID)
                                             .withExecutionId(ACTIVITY_ID)
                                             .withHost(PUBLIC_DNS)
                                             .withUserName(SSH_USER_NAME)
                                             .withExecutorType(KEY_AUTH)
                                             .withKey(WingsTestConstants.SSH_KEY)
                                             .withKeyName(HOST_CONN_ATTR_KEY.getUuid())
                                             .withAccountId(ACCOUNT_ID)
                                             .build();

    when(sshExecutorFactory.getExecutor(expectedSshConfig)).thenReturn(scriptSshExecutor);
    sshCommandUnitExecutorService.execute(EXEC_COMMAND_UNIT,
        commandExecutionContextBuider.but().withHostConnectionAttributes(HOST_CONN_ATTR_KEY).build());
    verify(sshExecutorFactory).getExecutor(expectedSshConfig);
  }

  /**
   * Should create bastion host based ssh config.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldCreateBastionHostBasedSshConfig() {
    SshSessionConfig expectedSshConfig = aSshSessionConfig()
                                             .withAppId(APP_ID)
                                             .withExecutionId(ACTIVITY_ID)
                                             .withHost(PUBLIC_DNS)
                                             .withUserName(SSH_USER_NAME)
                                             .withExecutorType(BASTION_HOST)
                                             .withAuthenticationScheme(SSH_KEY)
                                             .withAccessType(USER_PASSWORD)
                                             .withSshPassword(SSH_USER_PASSWORD)
                                             .withBastionHostConfig(aSshSessionConfig()
                                                                        .withHost(HOST_NAME)
                                                                        .withKey(WingsTestConstants.SSH_KEY)
                                                                        .withKeyName(BASTION_HOST_ATTR.getUuid())
                                                                        .build())
                                             .withAccountId(ACCOUNT_ID)
                                             .build();

    when(sshExecutorFactory.getExecutor(expectedSshConfig)).thenReturn(scriptSshExecutor);
    sshCommandUnitExecutorService.execute(EXEC_COMMAND_UNIT,
        commandExecutionContextBuider.but()
            .withHostConnectionAttributes(HOST_CONN_ATTR_PWD)
            .withBastionConnectionAttributes(BASTION_HOST_ATTR)
            .build());
    verify(sshExecutorFactory).getExecutor(expectedSshConfig);
  }

  /**
   * Should execute exec command.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldExecuteExecCommand() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid()).build();
    when(sshExecutorFactory.getExecutor(any(SshSessionConfig.class))).thenReturn(scriptSshExecutor);
    sshCommandUnitExecutorService.execute(EXEC_COMMAND_UNIT,
        commandExecutionContextBuider.but().withHostConnectionAttributes(HOST_CONN_ATTR_PWD).build());
    verify(scriptSshExecutor).executeCommandString(EXEC_COMMAND_UNIT.getPreparedCommand(), false);
  }

  /**
   * Should execute copy command.
   */
  @Test
  @Owner(developers = ANUBHAW)
  @Category(UnitTests.class)
  public void shouldExecuteCopyCommand() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid()).build();
    ScpCommandUnit commandUnit = aScpCommandUnit()
                                     .withCommandUnitType(CommandUnitType.SCP)
                                     .withDestinationDirectoryPath(FILE_PATH)
                                     .withFileCategory(ScpFileCategory.ARTIFACTS)
                                     .build();

    when(sshExecutorFactory.getExecutor(any(SshSessionConfig.class))).thenReturn(scriptSshExecutor);
    ArtifactStreamAttributes artifactStreamAttributes = ArtifactStreamAttributes.builder().metadataOnly(false).build();
    sshCommandUnitExecutorService.execute(commandUnit,
        commandExecutionContextBuider.but()
            .withHostConnectionAttributes(HOST_CONN_ATTR_PWD)
            .withArtifactStreamAttributes(artifactStreamAttributes)
            .build());
    verify(scriptSshExecutor)
        .copyGridFsFiles(commandUnit.getDestinationDirectoryPath(), ARTIFACTS,
            Lists.newArrayList(org.apache.commons.lang3.tuple.Pair.of(FILE_ID, null)));
  }

  /**
   * Should execute init command.
   *
   * @throws IOException the io exception
   */
  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldExecuteInitCommand() throws IOException {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid()).build();
    InitSshCommandUnit commandUnit = new InitSshCommandUnit();
    on(commandUnit).set("commandUnitHelper", new CommandUnitHelper());
    Command command =
        aCommand()
            .withCommandUnits(asList(commandUnit,
                anExecCommandUnit().withName("dols").withCommandPath("/tmp").withCommandString("ls").build()))
            .build();
    commandUnit.setCommand(command);

    when(sshExecutorFactory.getExecutor(any(SshSessionConfig.class))).thenReturn(scriptSshExecutor);
    when(scriptSshExecutor.executeCommandString(anyString(), anyBoolean())).thenReturn(CommandExecutionStatus.SUCCESS);
    when(scriptSshExecutor.copyFiles(anyString(), anyListOf(String.class))).thenReturn(CommandExecutionStatus.SUCCESS);

    sshCommandUnitExecutorService.execute(
        commandUnit, commandExecutionContextBuider.but().withHostConnectionAttributes(HOST_CONN_ATTR_PWD).build());
    verify(scriptSshExecutor).executeCommandString("mkdir -p /tmp/ACTIVITY_ID", false);

    String actualLauncherScript =
        new File(System.getProperty("java.io.tmpdir"), "harnesslauncherACTIVITY_ID.sh").getAbsolutePath();
    verify(scriptSshExecutor).copyFiles("/tmp/ACTIVITY_ID", asList(actualLauncherScript));
    assertThat(new File(actualLauncherScript))
        .hasContent(
            CharStreams.toString(new InputStreamReader(getClass().getResourceAsStream("/expectedLaunchScript.sh"))));

    String expectedExecCommandUnitScript =
        new File(System.getProperty("java.io.tmpdir"), "harness" + DigestUtils.md5Hex("dolsACTIVITY_ID"))
            .getAbsolutePath();
    verify(scriptSshExecutor).copyFiles("/tmp/ACTIVITY_ID", asList(expectedExecCommandUnitScript));
    verify(scriptSshExecutor).executeCommandString("chmod 0700 /tmp/ACTIVITY_ID/*", false);

    assertThat(new File(expectedExecCommandUnitScript)).hasContent("ls");
    assertThat((ExecCommandUnit) command.getCommandUnits().get(1))
        .extracting(ExecCommandUnit::getPreparedCommand)
        .isEqualTo(
            "/tmp/ACTIVITY_ID/harnesslauncherACTIVITY_ID.sh -w '/tmp' harness" + DigestUtils.md5Hex("dolsACTIVITY_ID"));
  }

  /**
   * Should execute init command.
   *
   */
  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldExecuteInitCommandV2() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid()).build();
    InitSshCommandUnitV2 commandUnit = new InitSshCommandUnitV2();
    on(commandUnit).set("commandUnitHelper", new CommandUnitHelper());
    Command command =
        aCommand()
            .withCommandUnits(asList(commandUnit,
                anExecCommandUnit().withName("dols").withCommandPath("/tmp").withCommandString("ls").build()))
            .build();
    commandUnit.setCommand(command);

    when(sshExecutorFactory.getExecutor(any(SshSessionConfig.class))).thenReturn(scriptSshExecutor);
    when(scriptSshExecutor.executeCommandString(anyString(), anyBoolean())).thenReturn(CommandExecutionStatus.SUCCESS);

    sshCommandUnitExecutorService.execute(commandUnit,
        commandExecutionContextBuider.but()
            .withHostConnectionAttributes(HOST_CONN_ATTR_PWD)
            .withInlineSshCommand(true)
            .build());
    verify(scriptSshExecutor).executeCommandString("mkdir -p /tmp/ACTIVITY_ID", false);
    assertThat(((ExecCommandUnit) command.getCommandUnits().get(1)).getPreparedCommand()).contains("ls");
  }

  /**
   * Should execute init command with nested units.
   *
   * @throws IOException the io exception
   */
  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldExecuteInitCommandWithNestedUnits() throws IOException {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid()).build();
    InitSshCommandUnit commandUnit = new InitSshCommandUnit();
    on(commandUnit).set("commandUnitHelper", new CommandUnitHelper());
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

    when(sshExecutorFactory.getExecutor(any(SshSessionConfig.class))).thenReturn(scriptSshExecutor);
    when(scriptSshExecutor.executeCommandString(anyString(), anyBoolean())).thenReturn(CommandExecutionStatus.SUCCESS);
    when(scriptSshExecutor.copyFiles(anyString(), anyListOf(String.class))).thenReturn(CommandExecutionStatus.SUCCESS);
    sshCommandUnitExecutorService.execute(
        commandUnit, commandExecutionContextBuider.but().withHostConnectionAttributes(HOST_CONN_ATTR_PWD).build());

    String expectedExecCommandUnitScript =
        new File(System.getProperty("java.io.tmpdir"), "harness" + DigestUtils.md5Hex("dolsACTIVITY_ID"))
            .getAbsolutePath();
    String expectedSubExecCommandUnitScript =
        new File(System.getProperty("java.io.tmpdir"), "harness" + DigestUtils.md5Hex("start1startscriptACTIVITY_ID"))
            .getAbsolutePath();

    verify(scriptSshExecutor)
        .copyFiles("/tmp/ACTIVITY_ID", asList(expectedExecCommandUnitScript, expectedSubExecCommandUnitScript));

    assertThat(new File(expectedExecCommandUnitScript)).hasContent("ls");
    assertThat((ExecCommandUnit) command.getCommandUnits().get(1))
        .extracting(ExecCommandUnit::getPreparedCommand)
        .isEqualTo(
            "/tmp/ACTIVITY_ID/harnesslauncherACTIVITY_ID.sh -w '/tmp' harness" + DigestUtils.md5Hex("dolsACTIVITY_ID"));

    assertThat(new File(expectedSubExecCommandUnitScript)).hasContent("start.sh");
    assertThat((ExecCommandUnit) ((Command) command.getCommandUnits().get(2)).getCommandUnits().get(0))
        .extracting(ExecCommandUnit::getPreparedCommand)
        .isEqualTo("/tmp/ACTIVITY_ID/harnesslauncherACTIVITY_ID.sh -w '/home/tomcat' harness"
            + DigestUtils.md5Hex("start1startscriptACTIVITY_ID"));

    verify(scriptSshExecutor).executeCommandString("chmod 0700 /tmp/ACTIVITY_ID/*", false);
  }

  /**
   * Should execute init command with nested units.
   *
   */
  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void shouldExecuteInitCommandV2WithNestedUnits() {
    Host host = builder.withHostConnAttr(HOST_CONN_ATTR_PWD.getUuid()).build();
    InitSshCommandUnitV2 commandUnit = new InitSshCommandUnitV2();
    on(commandUnit).set("commandUnitHelper", new CommandUnitHelper());
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

    when(sshExecutorFactory.getExecutor(any(SshSessionConfig.class))).thenReturn(scriptSshExecutor);
    when(scriptSshExecutor.executeCommandString(anyString(), anyBoolean())).thenReturn(CommandExecutionStatus.SUCCESS);
    sshCommandUnitExecutorService.execute(commandUnit,
        commandExecutionContextBuider.but()
            .withHostConnectionAttributes(HOST_CONN_ATTR_PWD)
            .withInlineSshCommand(true)
            .build());

    assertThat(((ExecCommandUnit) command.getCommandUnits().get(1)).getPreparedCommand()).contains("ls");
    assertThat(
        ((ExecCommandUnit) ((Command) command.getCommandUnits().get(2)).getCommandUnits().get(0)).getPreparedCommand())
        .contains("start.sh");

    verify(scriptSshExecutor).executeCommandString("mkdir -p /tmp/ACTIVITY_ID", false);
  }
}
