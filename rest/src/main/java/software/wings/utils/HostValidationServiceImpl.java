package software.wings.utils;

import static org.awaitility.Awaitility.with;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.utils.SshHelperUtil.getSshSessionConfig;
import static software.wings.utils.SshHelperUtil.normalizeError;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.ssh.executors.SshSessionFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.ExecutionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class HostValidationServiceImpl implements HostValidationService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private EncryptionService encryptionService;

  public List<HostValidationResponse> validateHost(List<String> hostNames, SettingAttribute connectionSetting,
      List<EncryptedDataDetail> encryptionDetails, ExecutionCredential executionCredential) {
    List<HostValidationResponse> hostValidationResponses = new ArrayList<>();

    encryptionService.decrypt((Encryptable) connectionSetting.getValue(), encryptionDetails);
    try {
      with().pollInterval(3L, TimeUnit.SECONDS).atMost(new Duration(60L, TimeUnit.SECONDS)).until(() -> {
        hostNames.forEach(hostName -> {
          CommandExecutionContext commandExecutionContext = aCommandExecutionContext()
                                                                .withHostConnectionAttributes(connectionSetting)
                                                                .withExecutionCredential(executionCredential)
                                                                .build();
          SshSessionConfig sshSessionConfig =
              getSshSessionConfig(hostName, "HOST_CONNECTION_TEST", commandExecutionContext, 60);

          HostValidationResponse response = HostValidationResponse.Builder.aHostValidationResponse()
                                                .withHostName(hostName)
                                                .withStatus(ExecutionStatus.SUCCESS.name())
                                                .build();
          try {
            Session sshSession = SshSessionFactory.getSSHSession(sshSessionConfig);
            sshSession.disconnect();
          } catch (JSchException jschEx) {
            ErrorCode errorCode = normalizeError(jschEx);
            response.setStatus(ExecutionStatus.FAILED.name());
            response.setErrorCode(errorCode.getCode());
            response.setErrorDescription(errorCode.getDescription());
          }
          hostValidationResponses.add(response);
        });
      });
    } catch (ConditionTimeoutException ex) {
      logger.warn("Host validation timed out", ex);
      // populate timeout error for rest of the hosts
      for (int idx = hostValidationResponses.size(); idx < hostNames.size(); idx++) {
        hostValidationResponses.add(HostValidationResponse.Builder.aHostValidationResponse()
                                        .withHostName(hostNames.get(idx))
                                        .withStatus(ExecutionStatus.FAILED.name())
                                        .withErrorCode(ErrorCode.REQUEST_TIMEOUT.getCode())
                                        .withErrorDescription(ErrorCode.REQUEST_TIMEOUT.getDescription())
                                        .build());
      }
    }
    return hostValidationResponses;
  }
}
