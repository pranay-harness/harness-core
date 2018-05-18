package software.wings.utils;

import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.utils.SshHelperUtil.getSshSessionConfig;
import static software.wings.utils.SshHelperUtil.normalizeError;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
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
  private static final Logger logger = LoggerFactory.getLogger(HostValidationServiceImpl.class);

  @Inject private EncryptionService encryptionService;
  @Inject private TimeLimiter timeLimiter;

  public List<HostValidationResponse> validateHost(List<String> hostNames, SettingAttribute connectionSetting,
      List<EncryptedDataDetail> encryptionDetails, ExecutionCredential executionCredential) {
    List<HostValidationResponse> hostValidationResponses = new ArrayList<>();

    encryptionService.decrypt((Encryptable) connectionSetting.getValue(), encryptionDetails);
    try {
      timeLimiter.callWithTimeout(() -> {
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
            response.setErrorCode(errorCode.name());
            response.setErrorDescription(errorCode.getDescription());
          }
          hostValidationResponses.add(response);
        });
        return true;
      }, 1, TimeUnit.MINUTES, true);
    } catch (UncheckedTimeoutException ex) {
      logger.warn("Host validation timed out", ex);
      // populate timeout error for rest of the hosts
      for (int idx = hostValidationResponses.size(); idx < hostNames.size(); idx++) {
        hostValidationResponses.add(HostValidationResponse.Builder.aHostValidationResponse()
                                        .withHostName(hostNames.get(idx))
                                        .withStatus(ExecutionStatus.FAILED.name())
                                        .withErrorCode(ErrorCode.REQUEST_TIMEOUT.name())
                                        .withErrorDescription(ErrorCode.REQUEST_TIMEOUT.getDescription())
                                        .build());
      }
    } catch (Exception ex) {
      logger.warn("Host validation failed", ex);
      // populate error for rest of the hosts
      for (int idx = hostValidationResponses.size(); idx < hostNames.size(); idx++) {
        hostValidationResponses.add(HostValidationResponse.Builder.aHostValidationResponse()
                                        .withHostName(hostNames.get(idx))
                                        .withStatus(ExecutionStatus.FAILED.name())
                                        .withErrorCode(ErrorCode.UNKNOWN_ERROR.name())
                                        .withErrorDescription(ErrorCode.UNKNOWN_ERROR.getDescription())
                                        .build());
      }
    }
    return hostValidationResponses;
  }
}
