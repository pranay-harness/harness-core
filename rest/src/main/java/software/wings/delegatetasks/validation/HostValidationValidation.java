package software.wings.delegatetasks.validation;

import static org.awaitility.Awaitility.with;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;
import static software.wings.utils.SshHelperUtil.getSshSessionConfig;

import com.google.inject.Inject;

import com.jcraft.jsch.JSchException;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import software.wings.annotation.Encryptable;
import software.wings.beans.DelegateTask;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class HostValidationValidation extends AbstractDelegateValidateTask {
  @Inject private EncryptionService encryptionService;

  public HostValidationValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    Object[] parameters = getParameters();
    return validateHosts((List<String>) parameters[2], (SettingAttribute) parameters[3],
        (List<EncryptedDataDetail>) parameters[4], (ExecutionCredential) parameters[5]);
  }

  private List<DelegateConnectionResult> validateHosts(List<String> hostNames, SettingAttribute connectionSetting,
      List<EncryptedDataDetail> encryptionDetails, ExecutionCredential executionCredential) {
    List<DelegateConnectionResult> results = new ArrayList<>();
    encryptionService.decrypt((Encryptable) connectionSetting.getValue(), encryptionDetails);
    try {
      with()
          .pollInterval(1L, TimeUnit.SECONDS)
          .atMost(new Duration(30L, TimeUnit.SECONDS))
          .until(() -> hostNames.forEach(hostName -> {
            DelegateConnectionResultBuilder resultBuilder = DelegateConnectionResult.builder().criteria(hostName);
            long startTime = System.currentTimeMillis();
            try {
              getSSHSession(getSshSessionConfig(hostName, "HOST_CONNECTION_TEST",
                                aCommandExecutionContext()
                                    .withHostConnectionAttributes(connectionSetting)
                                    .withExecutionCredential(executionCredential)
                                    .build(),
                                20))
                  .disconnect();
              resultBuilder.validated(true);
            } catch (JSchException jschEx) {
              // Invalid credentials error is still a valid connection
              resultBuilder.validated(StringUtils.contains(jschEx.getMessage(), "Auth"));
            }
            results.add(resultBuilder.duration(System.currentTimeMillis() - startTime).build());
          }));
    } catch (ConditionTimeoutException ex) {
      // Do nothing
    }
    return results;
  }

  @Override
  public List<String> getCriteria() {
    return (List<String>) getParameters()[2];
  }
}
