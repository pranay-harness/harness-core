package software.wings.core.ssh.executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.wings.beans.ErrorConstants.INVALID_CREDENTIAL;
import static software.wings.beans.ErrorConstants.INVALID_PORT;
import static software.wings.beans.ErrorConstants.UNKNOWN_HOST;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.core.ssh.executors.SshExecutor.ExecutionResult.FAILURE;
import static software.wings.core.ssh.executors.SshExecutor.ExecutionResult.SUCCESS;
import static software.wings.core.ssh.executors.SshSessionConfig.SshSessionConfigBuilder.aSshSessionConfig;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.core.ssh.executors.SshExecutor.ExecutionResult;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ExecutionLogs;
import software.wings.service.intfc.FileService;

/**
 * Created by anubhaw on 2/10/16.
 */

/*
1.  Successfully connect
2.  Fail for unknown hosts
3.  fail for invalid port
4.  fail for bad user credentials
5.  fail for connection timeout
6.  fail for command timeout
7.  fail for too much data written
8.  test for logging collected
9.  transfer file successfully
10. fail to transfer too big file
11. single/multi line banner handling
12. Remote connection closed handling
13. Remote connection closed resume
14. Successfully release channel on success/failure/exceptions
*/

public class SshPwdAuthExecutorTest extends WingsBaseTest {
  private final String HOST = "192.168.1.47";
  private final Integer PORT = 22;
  private final String USER = "ssh_user";
  private final String PASSWORD = "Wings@123";
  private final String EXECUTION_ID = "EXECUTION_ID";
  private SshSessionConfig config;
  private SshExecutor executor;

  @Mock private FileService fileService;
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
  public void shouldReturnSuccessForSuccessfulCommandExecution() throws Exception {
    executor.init(config);
    String fileName = getUuid();
    ExecutionResult execute = executor.execute(String.format("touch %s && rm %s", fileName, fileName));
    assertThat(execute).isEqualTo(SUCCESS);
  }

  @Test
  public void shouldReturnFailureForFailedCommandExecution() throws Exception {
    executor.init(config);
    ExecutionResult execute = executor.execute(String.format("rm %s", "FILE_DOES_NOT_EXIST"));
    assertThat(execute).isEqualTo(FAILURE);
  }

  @Ignore
  @Test
  public void testSCP() throws Exception {
    SshSessionConfig config = aSshSessionConfig()
                                  .withExecutionId(getUuid())
                                  .withExecutorType(ExecutorType.PASSWORD)
                                  .withHost("localhost")
                                  .withPort(2222)
                                  .withUser("osboxes")
                                  .withPassword("osboxes.org")
                                  .build();

    SshExecutor executor = SshExecutorFactory.getExecutor(config);
    String fileName = "mvim";
    ExecutionResult result = executor.transferFile("/Users/anubhaw/Downloads/" + fileName, "./" + fileName);
    System.out.println(result);
  }
}
