package software.wings.core.ssh.executors;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.beans.command.CommandUnit.ExecutionResult.FAILURE;
import static software.wings.beans.command.CommandUnit.ExecutionResult.SUCCESS;
import static software.wings.beans.ConfigFile.Builder.aConfigFile;
import static software.wings.beans.ErrorCodes.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorCodes.INVALID_PORT;
import static software.wings.beans.ErrorCodes.SOCKET_CONNECTION_TIMEOUT;
import static software.wings.beans.ErrorCodes.SSH_SESSION_TIMEOUT;
import static software.wings.beans.ErrorCodes.UNKNOWN_HOST;
import static software.wings.beans.command.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.wings.WingsBaseTest;
import software.wings.beans.command.CommandUnit.ExecutionResult;
import software.wings.beans.ConfigFile;
import software.wings.beans.command.ScpCommandUnit;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.exception.WingsException;
import software.wings.rules.Integration;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.LogService;

import java.io.BufferedWriter;
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

@Integration
@Ignore
public class SshPwdAuthExecutorTest extends WingsBaseTest {
  private final String HOST = "192.168.1.106";
  private final Integer PORT = 22;
  private final String USER = "ssh_user";
  private final String PASSWORD = "Wings@123";
  private final String EXECUTION_ID = "EXECUTION_ID";
  /**
   * The Test folder.
   */
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();
  private SshSessionConfig config;
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
    config = aSshSessionConfig()
                 .withAppId("APP_ID")
                 .withExecutionId(getUuid())
                 .withExecutorType(ExecutorType.PASSWORD_AUTH)
                 .withHost(HOST)
                 .withPort(PORT)
                 .withUserName(USER)
                 .withPassword(PASSWORD)
                 .withSshConnectionTimeout(5000)
                 .build();
  }

  /**
   * Should connect to remote host.
   */
  @Test
  public void shouldConnectToRemoteHost() {
    executor.init(config);
  }

  /**
   * Should throw unknown host exception for invalid host.
   */
  @Test
  public void shouldThrowUnknownHostExceptionForInvalidHost() {
    config.setHost("INVALID_HOST");
    assertThatThrownBy(() -> executor.init(config))
        .isInstanceOf(WingsException.class)
        .hasMessage(UNKNOWN_HOST.getCode());
  }

  /**
   * Should throw unknown host exception for invalid port.
   */
  @Test
  public void shouldThrowUnknownHostExceptionForInvalidPort() {
    config.setPort(3333);
    assertThatThrownBy(() -> executor.init(config))
        .isInstanceOf(WingsException.class)
        .hasMessage(INVALID_PORT.getCode());
  }

  /**
   * Should throw exception for invalid credential.
   */
  @Test
  public void shouldThrowExceptionForInvalidCredential() {
    config.setPassword("INVALID_PASSWORD");
    Assertions.assertThatThrownBy(() -> executor.init(config))
        .isInstanceOf(WingsException.class)
        .hasMessageContaining(INVALID_CREDENTIAL.getCode());
  }

  /**
   * Should return success for successful command execution.
   */
  @Test
  public void shouldReturnSuccessForSuccessfulCommandExecution() {
    executor.init(config);
    String fileName = getUuid();
    ExecutionResult execute = executor.execute(
        anExecCommandUnit().withCommandString(String.format("touch %s && rm %s", fileName, fileName)).build());
    assertThat(execute).isEqualTo(SUCCESS);
  }

  /**
   * Should return failure for failed command execution.
   */
  @Test
  public void shouldReturnFailureForFailedCommandExecution() {
    executor.init(config);
    ExecutionResult execute =
        executor.execute(anExecCommandUnit().withCommandString(String.format("rm %s", "FILE_DOES_NOT_EXIST")).build());
    assertThat(execute).isEqualTo(FAILURE);
  }

  /**
   * Should throw exception for connection timeout.
   */
  @Test
  public void shouldThrowExceptionForConnectionTimeout() {
    config.setSshConnectionTimeout(1); // 1ms
    assertThatThrownBy(() -> executor.init(config))
        .isInstanceOf(WingsException.class)
        .hasMessage(SOCKET_CONNECTION_TIMEOUT.getCode());
  }

  /**
   * Should throw exception for session timeout.
   */
  @Test
  public void shouldThrowExceptionForSessionTimeout() {
    config.setSshSessionTimeout(1); // 1ms
    executor.init(config);
    assertThatThrownBy(() -> executor.execute(anExecCommandUnit().withCommandString("ls -lh").build()))
        .isInstanceOf(WingsException.class)
        .hasMessage(SSH_SESSION_TIMEOUT.getCode());
  }

  /**
   * Test scp.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Test
  public void shouldTransferFile() throws IOException {
    File file = testFolder.newFile();
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    out.write("ANY_TEXT");
    out.close();

    ConfigFile appConfigFile = aConfigFile()
                                   .withName("FILE_NAME")
                                   .withTemplateId("TEMPLATE_ID")
                                   .withEntityId("ENTITY_ID")
                                   .withRelativePath("/configs/")
                                   .build();
    FileInputStream fileInputStream = new FileInputStream(file);
    String fileId = fileService.saveFile(appConfigFile, fileInputStream, CONFIGS);
    executor.init(config);
    String fileName = "mvim";
    ExecutionResult result = executor.execute(ScpCommandUnit.Builder.aScpCommandUnit()
                                                  .withFileIds(asList(fileId))
                                                  .withDestinationDirectoryPath("./" + fileName)
                                                  .withFileBucket(CONFIGS)
                                                  .build());
  }

  /**
   * Should concat paths.
   */
  @Test
  public void shouldConcatPaths() {}
}
