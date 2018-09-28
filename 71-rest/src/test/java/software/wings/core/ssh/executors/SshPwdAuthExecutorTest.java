package software.wings.core.ssh.executors;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.eraro.ErrorCode.INVALID_CREDENTIAL;
import static io.harness.eraro.ErrorCode.SOCKET_CONNECTION_ERROR;
import static io.harness.eraro.ErrorCode.SOCKET_CONNECTION_TIMEOUT;
import static io.harness.eraro.ErrorCode.SSH_SESSION_TIMEOUT;
import static io.harness.eraro.ErrorCode.UNKNOWN_HOST;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;
import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.FILE_ID;

import com.google.common.io.CharStreams;

import io.harness.exception.WingsException;
import io.harness.rule.RepeatRule.Repeat;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ConfigFile;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.rules.SshRule;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * Created by anubhaw on 2/10/16.
 */
/*
-1.  Successfully connect
-2.  Fail for unknown hosts
-3.  fail for invalid port
-4.  fail for bad user credentials
-5.  fail for connection timeout
-6.  fail for command timeout
7.  fail for too much data written
8.  test for logging collected
-9.  transfer file successfully
10. fail to transfer too big file
11. single/multi line banner handling
12. Remote connection closed handling
13. Remote connection closed resume
14. Successfully release channel on success/failure/exceptions
-15. Return Success status on successful command execution
-16. Return Failure status on failed command execution
17. Sudo app user
18. su app user
*/

public class SshPwdAuthExecutorTest extends WingsBaseTest {
  private static final String HOST = "localhost";
  /**
   * The Test folder.
   */
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();
  /**
   * The Ssh root.
   */
  @Rule public TemporaryFolder sshRoot = new TemporaryFolder();
  /**
   * The Ssh rule.
   */
  @Rule public SshRule sshRule = new SshRule(sshRoot);
  private SshSessionConfig.Builder configBuilder;
  private SshExecutor executor;
  @Mock private DelegateFileManager fileService;
  @Mock private DelegateLogService logService;

  /**
   * Sets the up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    executor = new SshPwdAuthExecutor(fileService, logService);
    configBuilder = aSshSessionConfig()
                        .withAppId("APP_ID")
                        .withExecutionId(generateUuid())
                        .withExecutorType(ExecutorType.PASSWORD_AUTH)
                        .withHost(HOST)
                        .withPort(sshRule.getPort())
                        .withUserName(sshRule.getUsername())
                        .withPassword(sshRule.getPassword().toCharArray())
                        .withSshConnectionTimeout(5000)
                        .withAccountId(ACCOUNT_ID)
                        .withCommandUnitName("test");
  }

  /**
   * Should connect to remote host.
   */
  @Test
  public void shouldConnectToRemoteHost() {
    executor.init(configBuilder.build());
  }

  /**
   * Should throw unknown host exception for invalid host.
   */
  @Test
  public void shouldThrowUnknownHostExceptionForInvalidHost() {
    executor.init(configBuilder.but().withHost("INVALID_HOST").build());
    assertThatThrownBy(() -> executor.executeCommandString("ls"))
        .isInstanceOf(WingsException.class)
        .hasMessage(UNKNOWN_HOST.name());
  }

  /**
   * Should throw unknown host exception for invalid port.
   */
  @Test
  public void shouldThrowUnknownHostExceptionForInvalidPort() {
    executor.init(configBuilder.but().withPort(3333).build());
    assertThatThrownBy(() -> executor.executeCommandString("ls"))
        .isInstanceOf(WingsException.class)
        .hasMessage(SOCKET_CONNECTION_ERROR.name());
  }

  /**
   * Should throw exception for invalid credential.
   */
  @Test
  @Ignore
  @Repeat(times = 3, successes = 1)
  public void shouldThrowExceptionForInvalidCredential() {
    executor.init(configBuilder.but().withPassword("INVALID_PASSWORD".toCharArray()).build());
    assertThatThrownBy(() -> executor.executeCommandString("ls"))
        .isInstanceOf(WingsException.class)
        .hasMessageContaining(INVALID_CREDENTIAL.name());
  }

  /**
   * Should return success for successful command execution.
   */
  @Test
  @Ignore
  // Too unstable to keep even with repeats
  public void shouldReturnSuccessForSuccessfulCommandExecution() {
    SshSessionConfig sshSessionConfig = configBuilder.but().build();

    executor.init(sshSessionConfig);
    String fileName = generateUuid();
    CommandExecutionStatus execute = executor.executeCommandString(format("touch %s && rm %s", fileName, fileName));
    assertEquals("ssh command result is " + execute.toString(), SUCCESS, execute);
  }

  /**
   * Should return failure for failed command execution.
   */
  @Test
  @Repeat(times = 3, successes = 1)
  public void shouldReturnFailureForFailedCommandExecution() {
    executor.init(configBuilder.build());
    CommandExecutionStatus execute = executor.executeCommandString(format("rm %s", "FILE_DOES_NOT_EXIST"));
    assertThat(execute).isEqualTo(FAILURE);
  }

  /**
   * Should throw exception for connection timeout.
   */
  @Test
  @Repeat(times = 3, successes = 1)
  public void shouldThrowExceptionForConnectionTimeout() {
    executor.init(configBuilder.but().withSshConnectionTimeout(1).build());
    assertThatThrownBy(() -> executor.executeCommandString("ls"))
        .isInstanceOf(WingsException.class)
        .hasMessage(SOCKET_CONNECTION_TIMEOUT.name());
  }

  /**
   * Should throw exception for session timeout.
   */
  @Ignore
  @Test
  public void shouldThrowExceptionForSessionTimeout() {
    executor.init(configBuilder.but().withSshSessionTimeout(1).build());
    assertThatThrownBy(() -> executor.executeCommandString("sleep 10"))
        .isInstanceOf(WingsException.class)
        .hasMessage(SSH_SESSION_TIMEOUT.name());
  }

  /**
   * Should throw exception for connect timeout.
   */
  @Test
  public void shouldThrowExceptionForConnectTimeout() {
    executor.init(configBuilder.but().withHost("host1.app.com").withPort(22).withSocketConnectTimeout(2000).build());
    assertThatThrownBy(() -> executor.executeCommandString("ls"))
        .isInstanceOf(WingsException.class)
        .hasMessage(SOCKET_CONNECTION_TIMEOUT.name());
  }

  /**
   * Test scp.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  @Repeat(times = 3, successes = 1)
  @Ignore
  public void shouldTransferGridFSFile() throws IOException, ExecutionException {
    File file = testFolder.newFile();
    CharStreams.asWriter(new FileWriter(file)).append("ANY_TEXT").close();

    ConfigFile appConfigFile =
        ConfigFile.builder().templateId("TEMPLATE_ID").entityId("ENTITY_ID").relativeFilePath("/configs/").build();
    appConfigFile.setName("FILE_NAME");
    appConfigFile.setFileName("text.txt");
    FileInputStream fileInputStream = new FileInputStream(file);
    when(fileService.getMetaInfo(any(FileBucket.class), anyString(), anyString()))
        .thenReturn(aDelegateFile().withFileName("text.txt").withLength(file.length()).build());
    when(fileService.downloadArtifactByFileId(any(FileBucket.class), anyString(), anyString(), eq(false)))
        .thenReturn(fileInputStream);
    executor.init(configBuilder.but().build());

    assertThat(executor.copyGridFsFiles("/", CONFIGS, asList(Pair.of(FILE_ID, null)))).isEqualTo(SUCCESS);

    assertThat(new File(sshRoot.getRoot(), "text.txt")).hasSameContentAs(file).canRead().canWrite();
  }

  /**
   * Should transfer grid fs file with different name.
   *
   * @throws IOException the io exception
   */
  @Test
  @Ignore
  @Repeat(times = 3, successes = 1)
  public void shouldTransferGridFSFileWithDifferentName() throws IOException, ExecutionException {
    File file = testFolder.newFile();
    CharStreams.asWriter(new FileWriter(file)).append("ANY_TEXT").close();

    ConfigFile appConfigFile =
        ConfigFile.builder().templateId("TEMPLATE_ID").entityId("ENTITY_ID").relativeFilePath("/configs/").build();
    appConfigFile.setName("FILE_NAME");
    appConfigFile.setFileName("text.txt");
    FileInputStream fileInputStream = new FileInputStream(file);
    when(fileService.getMetaInfo(any(FileBucket.class), anyString(), anyString()))
        .thenReturn(aDelegateFile().withFileName("text.txt").withLength(file.length()).build());
    when(fileService.downloadArtifactByFileId(any(FileBucket.class), anyString(), anyString(), eq(false)))
        .thenReturn(fileInputStream);
    executor.init(configBuilder.but().build());

    assertThat(executor.copyGridFsFiles("/", CONFIGS, asList(Pair.of(FILE_ID, "text1.txt")))).isEqualTo(SUCCESS);

    assertThat(new File(sshRoot.getRoot(), "text1.txt")).hasSameContentAs(file).canRead().canWrite();
  }

  /**
   * Should transfer file.
   *
   * @throws IOException the io exception
   */
  @Test
  @Ignore
  @Repeat(times = 3, successes = 1)
  public void shouldTransferFile() throws IOException {
    File file = testFolder.newFile();
    CharStreams.asWriter(new FileWriter(file)).append("ANY_TEXT").close();

    executor.init(configBuilder.but().build());

    assertThat(executor.copyFiles("/tmp/", asList(file.getAbsolutePath()))).isEqualTo(SUCCESS);

    assertThat(new File(sshRoot.getRoot(), file.getName())).hasSameContentAs(file).canRead().canWrite();
  }
}
