package software.wings.core.ssh.executors;

import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.PASSWORD_AUTH;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.ExecutionLogs;
import software.wings.service.intfc.FileService;

import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/18/16.
 */

public class SshExecutorFactoryTest extends WingsBaseTest {
  @Mock FileService fileService;
  @Mock ExecutionLogs executionLogs;

  @Inject @InjectMocks SshExecutorFactory sshExecutorFactory;

  /**
   * Should get password based executor.
   */
  @Test
  public void shouldGetPasswordBasedExecutor() {
    SshExecutor executor = sshExecutorFactory.getExecutor(PASSWORD_AUTH);
    Assertions.assertThat(executor).isInstanceOf(SshPwdAuthExecutor.class);
  }
}
