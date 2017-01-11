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
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

/**
 * Created by anubhaw on 5/18/16.
 */
@RunWith(JUnitParamsRunner.class)
public class SshExecutorFactoryTest {
  /**
   * The File service.
   */
  @Mock DelegateFileManager fileService;
  /**
   * The Log service.
   */
  @Mock DelegateLogService logService;

  /**
   * The Ssh executor factory.
   */
  @InjectMocks private SshExecutorFactory sshExecutorFactory = new SshExecutorFactory();

  /**
   * Params object [ ].
   *
   * @return the object [ ]
   */
  public Object[] params() {
    return new Object[][] {{PASSWORD_AUTH, SshPwdAuthExecutor.class, "Password"},
        {KEY_AUTH, SshPubKeyAuthExecutor.class, "Key"}, {BASTION_HOST, SshJumpboxExecutor.class, "BastionHost"}};
  }

  /**
   * Should get password based executor.
   *
   * @param executorType the executor type
   * @param klass        the klass
   * @param name         the name
   */
  @Test
  @Parameters(method = "params")
  @TestCaseName("{method}{2}")
  public void shouldGetExecutorFor(ExecutorType executorType, Class<? extends AbstractSshExecutor> klass, String name) {
    assertThat(sshExecutorFactory.getExecutor(executorType)).isInstanceOf(klass);
  }
}
