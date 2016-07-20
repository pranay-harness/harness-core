package software.wings.core.ssh.executors;

import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.PASSWORD_AUTH;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.LogService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 5/18/16.
 */
public class SshExecutorFactoryTest extends WingsBaseTest {
  /**
   * The File service.
   */
  @Mock FileService fileService;
  /**
   * The Log service.
   */
  @Mock LogService logService;

  /**
   * The Ssh executor factory.
   */
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
