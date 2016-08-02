package software.wings.core.ssh.executors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.beans.ConfigFile.Builder.aConfigFile;
import static software.wings.beans.ErrorCodes.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorCodes.INVALID_PORT;
import static software.wings.beans.ErrorCodes.SOCKET_CONNECTION_TIMEOUT;
import static software.wings.beans.ErrorCodes.SSH_SESSION_TIMEOUT;
import static software.wings.beans.ErrorCodes.UNKNOWN_HOST;
import static software.wings.beans.command.CommandUnit.ExecutionResult.FAILURE;
import static software.wings.beans.command.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import com.google.common.io.CharStreams;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.wings.WingsBaseTest;
import software.wings.beans.ConfigFile;
import software.wings.beans.command.CommandUnit.ExecutionResult;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.exception.WingsException;
import software.wings.rules.RealMongo;
import software.wings.rules.SshRule;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.LogService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import javax.inject.Inject;

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
  private final String HOST = "localhost";
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
  @Inject private FileService fileService;
  @Inject private LogService logService;

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
                        .withExecutionId(getUuid())
                        .withExecutorType(ExecutorType.PASSWORD_AUTH)
                        .withHost(HOST)
                        .withPort(sshRule.getPort())
                        .withUserName(sshRule.getUsername())
                        .withPassword(sshRule.getPassword())
                        .withSshConnectionTimeout(5000)
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
        .hasMessage(UNKNOWN_HOST.getCode());
  }

  /**
   * Should throw unknown host exception for invalid port.
   */
  @Test
  public void shouldThrowUnknownHostExceptionForInvalidPort() {
    executor.init(configBuilder.but().withPort(3333).build());
    assertThatThrownBy(() -> executor.executeCommandString("ls"))
        .isInstanceOf(WingsException.class)
        .hasMessage(INVALID_PORT.getCode());
  }

  /**
   * Should throw exception for invalid credential.
   */
  @Test
  public void shouldThrowExceptionForInvalidCredential() {
    executor.init(configBuilder.but().withPassword("INVALID_PASSWORD").build());
    Assertions.assertThatThrownBy(() -> executor.executeCommandString("ls"))
        .isInstanceOf(WingsException.class)
        .hasMessageContaining(INVALID_CREDENTIAL.getCode());
  }

  /**
   * Should return success for successful command execution.
   */
  @Test
  public void shouldReturnSuccessForSuccessfulCommandExecution() {
    SshSessionConfig sshSessionConfig = configBuilder.but().build();

    executor.init(sshSessionConfig);
    String fileName = getUuid();
    ExecutionResult execute = executor.executeCommandString(String.format("touch %s && rm %s", fileName, fileName));
    assertThat(execute).isEqualTo(SUCCESS);
  }

  /**
   * Should return failure for failed command execution.
   */
  @Test
  public void shouldReturnFailureForFailedCommandExecution() {
    executor.init(configBuilder.build());
    ExecutionResult execute = executor.executeCommandString(String.format("rm %s", "FILE_DOES_NOT_EXIST"));
    assertThat(execute).isEqualTo(FAILURE);
  }

  /**
   * Should throw exception for connection timeout.
   */
  @Test
  public void shouldThrowExceptionForConnectionTimeout() {
    executor.init(configBuilder.but().withSshConnectionTimeout(1).build());
    assertThatThrownBy(() -> executor.executeCommandString("ls"))
        .isInstanceOf(WingsException.class)
        .hasMessage(SOCKET_CONNECTION_TIMEOUT.getCode());
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
        .hasMessage(SSH_SESSION_TIMEOUT.getCode());
  }

  /**
   * Should throw exception for connect timeout.
   */
  @Test
  public void shouldThrowExceptionForConnectTimeout() {
    executor.init(configBuilder.but().withHost("host1.app.com").withPort(22).withSocketConnectTimeout(2000).build());
    assertThatThrownBy(() -> executor.executeCommandString("ls"))
        .isInstanceOf(WingsException.class)
        .hasMessage(SOCKET_CONNECTION_TIMEOUT.getCode());
  }

  /**
   * Test scp.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @RealMongo
  @Test
  public void shouldTransferGridFSFile() throws IOException {
    File file = testFolder.newFile();
    CharStreams.asWriter(new FileWriter(file)).append("ANY_TEXT").close();

    ConfigFile appConfigFile = aConfigFile()
                                   .withName("FILE_NAME")
                                   .withTemplateId("TEMPLATE_ID")
                                   .withEntityId("ENTITY_ID")
                                   .withRelativePath("/configs/")
                                   .withFileName("text.txt")
                                   .build();
    FileInputStream fileInputStream = new FileInputStream(file);
    String fileId = fileService.saveFile(appConfigFile, fileInputStream, CONFIGS);
    executor.init(configBuilder.but().build());

    assertThat(executor.scpGridFsFiles("/", CONFIGS, asList(fileId))).isEqualTo(SUCCESS);

    assertThat(new File(sshRoot.getRoot(), "text.txt")).hasSameContentAs(file).canRead().canWrite();
  }

  /**
   * Should transfer file.
   *
   * @throws IOException the io exception
   */
  @Test
  public void shouldTransferFile() throws IOException {
    File file = testFolder.newFile();
    CharStreams.asWriter(new FileWriter(file)).append("ANY_TEXT").close();

    executor.init(configBuilder.but().build());

    assertThat(executor.scpFiles("/", asList(file.getAbsolutePath()))).isEqualTo(SUCCESS);

    assertThat(new File(sshRoot.getRoot(), file.getName())).hasSameContentAs(file).canRead().canWrite();
  }
}
