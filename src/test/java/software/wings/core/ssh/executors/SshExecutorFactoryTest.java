package software.wings.core.ssh.executors;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.BASTION_HOST;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.KEY_AUTH;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.PASSWORD_AUTH;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.LogService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 5/18/16.
 */
@RunWith(JUnitParamsRunner.class)
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
  @Inject @InjectMocks private SshExecutorFactory sshExecutorFactory;

  public Object[] params() {
    return new Object[][] {{PASSWORD_AUTH, SshPwdAuthExecutor.class, "Password"},
        {KEY_AUTH, SshPubKeyAuthExecutor.class, "Key"}, {BASTION_HOST, SshJumpboxExecutor.class, "BastionHost"}};
  }

  /**
   * Should get password based executor.
   */
  @Test
  @Parameters(method = "params")
  @TestCaseName("{method}{2}")
  public void shouldGetExecutorFor(ExecutorType executorType, Class<? extends AbstractSshExecutor> klass, String name) {
    assertThat(sshExecutorFactory.getExecutor(executorType)).isInstanceOf(klass);
  }
}
