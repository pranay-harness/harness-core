package software.wings.core.ssh.executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.beans.ConfigFile.ConfigFileBuilder.aConfigFile;
import static software.wings.beans.ErrorConstants.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorConstants.INVALID_PORT;
import static software.wings.beans.ErrorConstants.SOCKET_CONNECTION_TIMEOUT;
import static software.wings.beans.ErrorConstants.SSH_SESSION_TIMEOUT;
import static software.wings.beans.ErrorConstants.UNKNOWN_HOST;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.core.ssh.executors.SshExecutor.ExecutionResult.FAILURE;
import static software.wings.core.ssh.executors.SshExecutor.ExecutionResult.SUCCESS;
import static software.wings.core.ssh.executors.SshSessionConfig.SshSessionConfigBuilder.aSshSessionConfig;
import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ConfigFile;
import software.wings.core.ssh.executors.SshExecutor.ExecutionResult;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.exception.WingsException;
import software.wings.rules.RealMongo;
import software.wings.service.intfc.ExecutionLogs;
import software.wings.service.intfc.FileService;

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
*/

@RealMongo
@Ignore
public class SshPwdAuthExecutorTest extends WingsBaseTest {
  private final String HOST = "192.168.137.92";
  private final Integer PORT = 22;
  private final String USER = "ssh_user";
  private final String PASSWORD = "Wings@123";
  private final String EXECUTION_ID = "EXECUTION_ID";
  private SshSessionConfig config;
  private SshExecutor executor;
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  @Inject private FileService fileService;
  @Mock private ExecutionLogs executionLogs;

  @Before
  public void setUp() throws Exception {
    executor = new SshPwdAuthExecutor(executionLogs, fileService);
    config = aSshSessionConfig()
                 .withExecutionId(EXECUTION_ID)
                 .withExecutorType(ExecutorType.PASSWORD)
                 .withHost(HOST)
                 .withPort(PORT)
                 .withUser(USER)
                 .withPassword(PASSWORD)
                 .withSshConnectionTimeout(5000)
                 .build();
  }

  @Test
  public void shouldConnectToRemoteHost() {
    executor.init(config);
  }

  @Test
  public void shouldThrowUnknownHostExceptionForInvalidHost() {
    config.setHost("INVALID_HOST");
    assertThatThrownBy(() -> executor.init(config)).isInstanceOf(WingsException.class).hasMessage(UNKNOWN_HOST);
  }

  @Test
  public void shouldThrowUnknownHostExceptionForInvalidPort() {
    config.setPort(3333);
    assertThatThrownBy(() -> executor.init(config)).isInstanceOf(WingsException.class).hasMessage(INVALID_PORT);
  }

  @Test
  public void shouldThrowExceptionForInvalidCredential() {
    config.setPassword("INVALID_PASSWORD");
    Assertions.assertThatThrownBy(() -> executor.init(config))
        .isInstanceOf(WingsException.class)
        .hasMessageContaining(INVALID_CREDENTIAL);
  }

  @Test
  public void shouldReturnSuccessForSuccessfulCommandExecution() {
    executor.init(config);
    String fileName = getUuid();
    ExecutionResult execute = executor.execute(String.format("touch %s && rm %s", fileName, fileName));
    assertThat(execute).isEqualTo(SUCCESS);
  }

  @Test
  public void shouldReturnFailureForFailedCommandExecution() {
    executor.init(config);
    ExecutionResult execute = executor.execute(String.format("rm %s", "FILE_DOES_NOT_EXIST"));
    assertThat(execute).isEqualTo(FAILURE);
  }

  @Test
  public void shouldThrowExceptionForConnectionTimeout() {
    config.setSshConnectionTimeout(1); // 1ms
    assertThatThrownBy(() -> executor.init(config))
        .isInstanceOf(WingsException.class)
        .hasMessage(SOCKET_CONNECTION_TIMEOUT);
  }

  @Test
  public void shouldThrowExceptionForSessionTimeout() {
    config.setSshSessionTimeout(1); // 1ms
    executor.init(config);
    assertThatThrownBy(() -> executor.execute("ls -lh"))
        .isInstanceOf(WingsException.class)
        .hasMessage(SSH_SESSION_TIMEOUT);
  }

  @Test
  public void testSCP() throws IOException {
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
    ExecutionResult result = executor.transferFile(fileId, "./" + fileName, CONFIGS);
    System.out.println(result);
  }
}
