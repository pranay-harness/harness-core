package software.wings.delegatetasks.validation;

import static io.harness.govern.Switch.unhandled;
import static java.util.Collections.singletonList;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;

import com.google.inject.Inject;

import com.jcraft.jsch.JSchException;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.winrm.executors.WinRmSession;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.WinRmHelperUtil;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

public class ShellScriptValidation extends AbstractDelegateValidateTask {
  @Inject @Transient private transient EncryptionService encryptionService;
  private static final Logger logger = LoggerFactory.getLogger(ShellScriptValidation.class);

  public ShellScriptValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    Object[] parameters = getParameters();
    return singletonList(validate((ShellScriptParameters) parameters[0]));
  }

  private DelegateConnectionResult validate(ShellScriptParameters parameters) {
    DelegateConnectionResultBuilder resultBuilder = DelegateConnectionResult.builder();
    resultBuilder.criteria(getCriteria().get(0));

    switch (parameters.getConnectionType()) {
      case SSH:
        try {
          SshSessionConfig expectedSshConfig = parameters.sshSessionConfig(encryptionService);
          getSSHSession(expectedSshConfig).disconnect();

          resultBuilder.validated(true);
        } catch (JSchException jschEx) {
          // Invalid credentials error is still a valid connection
          resultBuilder.validated(StringUtils.contains(jschEx.getMessage(), "Auth"));
        } catch (IOException ex) {
          resultBuilder.validated(false);
        }
        break;

      case WINRM:
        try {
          WinRmSessionConfig winrmConfig = parameters.winrmSessionConfig(encryptionService);
          try (WinRmSession session = new WinRmSession(winrmConfig)) {
            resultBuilder.validated(true);
          }
        } catch (Exception e) {
          logger.error(WinRmHelperUtil.HandleWinRmClientException(e), e);
          resultBuilder.validated(false);
        }
        break;

      default:
        unhandled(parameters.getConnectionType());
        resultBuilder.validated(false);
    }

    return resultBuilder.build();
  }

  @Override
  public List<String> getCriteria() {
    ShellScriptParameters parameters = (ShellScriptParameters) getParameters()[0];
    return singletonList(parameters.getHost());
  }
}
