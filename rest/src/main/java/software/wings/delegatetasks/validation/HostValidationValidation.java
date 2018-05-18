package software.wings.delegatetasks.validation;

import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;
import static software.wings.utils.SshHelperUtil.getSshSessionConfig;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;

import com.jcraft.jsch.JSchException;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.Encryptable;
import software.wings.beans.DelegateTask;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
public class HostValidationValidation extends AbstractDelegateValidateTask {
  @Inject @Transient private transient EncryptionService encryptionService;
  @Inject @Transient private transient TimeLimiter timeLimiter;
  @Inject @Transient private transient Clock clock;

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
      timeLimiter.callWithTimeout(() -> {
        for (String hostName : hostNames) {
          DelegateConnectionResultBuilder resultBuilder = DelegateConnectionResult.builder().criteria(hostName);
          long startTime = clock.millis();
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
          } catch (Exception e) {
            resultBuilder.validated(false);
          }
          results.add(resultBuilder.duration(clock.millis() - startTime).build());
        }
        return true;
      }, 30L, TimeUnit.SECONDS, true);
    } catch (Exception e) {
      // Do nothing
    }
    return results;
  }

  @Override
  public List<String> getCriteria() {
    return (List<String>) getParameters()[2];
  }
}
