package software.wings.delegatetasks.validation;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static software.wings.beans.command.CommandExecutionContext.Builder.aCommandExecutionContext;
import static software.wings.common.Constants.WINDOWS_HOME_DIR;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;
import static software.wings.utils.SshHelperUtils.createSshSessionConfig;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.HostValidationTaskParameters;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.beans.infrastructure.Host;
import software.wings.core.winrm.executors.WinRmSession;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.service.intfc.security.EncryptionService;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Created by brett on 11/2/17
 */
@Slf4j
public class HostValidationValidation extends AbstractDelegateValidateTask {
  public static final String BATCH_HOST_VALIDATION = "BATCH_HOST_VALIDATION:";
  @Inject private transient EncryptionService encryptionService;
  @Inject private transient TimeLimiter timeLimiter;
  @Inject private transient Clock clock;

  public HostValidationValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    Object[] parameters = getParameters();
    // TODO: temp change. Should go away when we enable delegate validation capability framework.
    HostValidationTaskParameters hostValidationTaskParameters = null;
    if (!(parameters[0] instanceof HostValidationTaskParameters)) {
      hostValidationTaskParameters = HostValidationTaskParameters.builder()
                                         .hostNames((List<String>) parameters[2])
                                         .connectionSetting((SettingAttribute) parameters[3])
                                         .encryptionDetails((List<EncryptedDataDetail>) parameters[4])
                                         .executionCredential((ExecutionCredential) parameters[5])
                                         .build();
    } else {
      hostValidationTaskParameters = (HostValidationTaskParameters) getParameters()[0];
    }

    return validateHosts(hostValidationTaskParameters.getHostNames(),
        hostValidationTaskParameters.getConnectionSetting(), hostValidationTaskParameters.getEncryptionDetails(),
        hostValidationTaskParameters.getExecutionCredential());
  }

  private List<DelegateConnectionResult> validateHosts(List<String> hostNames, SettingAttribute connectionSetting,
      List<EncryptedDataDetail> encryptionDetails, ExecutionCredential executionCredential) {
    List<DelegateConnectionResult> results = new ArrayList<>();
    encryptionService.decrypt((EncryptableSetting) connectionSetting.getValue(), encryptionDetails);
    try {
      timeLimiter.callWithTimeout(() -> {
        for (String hostName : hostNames) {
          DelegateConnectionResultBuilder resultBuilder =
              DelegateConnectionResult.builder().criteria(addPrefix(hostName));
          long startTime = clock.millis();
          if (connectionSetting.getValue() instanceof WinRmConnectionAttributes) {
            WinRmConnectionAttributes connectionAttributes = (WinRmConnectionAttributes) connectionSetting.getValue();
            WinRmSessionConfig config = WinRmSessionConfig.builder()
                                            .hostname(hostName)
                                            .commandUnitName("HOST_CONNECTION_TEST")
                                            .domain(connectionAttributes.getDomain())
                                            .username(connectionAttributes.getUsername())
                                            .password(String.valueOf(connectionAttributes.getPassword()))
                                            .authenticationScheme(connectionAttributes.getAuthenticationScheme())
                                            .port(connectionAttributes.getPort())
                                            .skipCertChecks(connectionAttributes.isSkipCertChecks())
                                            .useSSL(connectionAttributes.isUseSSL())
                                            .workingDirectory(WINDOWS_HOME_DIR)
                                            .environment(Collections.emptyMap())
                                            .build();
            logger.info("Validating WinrmSession to Host: {}, Port: {}, useSsl: {}", config.getHostname(),
                config.getPort(), config.isUseSSL());

            try (WinRmSession ignore = new WinRmSession(config)) {
              resultBuilder.validated(true);
            } catch (Exception e) {
              logger.info("Exception in WinrmSession Validation: {}", e);
              resultBuilder.validated(false);
            }
          } else {
            try {
              getSSHSession(createSshSessionConfig("HOST_CONNECTION_TEST",
                                aCommandExecutionContext()
                                    .hostConnectionAttributes(connectionSetting)
                                    .executionCredential(executionCredential)
                                    .host(Host.Builder.aHost().withHostName(hostName).withPublicDns(hostName).build())
                                    .build()))
                  .disconnect();
              resultBuilder.validated(true);
            } catch (Exception e) {
              resultBuilder.validated(false);
            }
          }
          results.add(resultBuilder.duration(clock.millis() - startTime).build());
        }

        return true;
      }, 30L, TimeUnit.SECONDS, true);
    } catch (Exception e) {
      // Do nothing
    }
    return prepareResult(results);
  }

  @VisibleForTesting
  List<DelegateConnectionResult> prepareResult(List<DelegateConnectionResult> delegateConnectionResults) {
    boolean anyValid = delegateConnectionResults.stream().anyMatch(DelegateConnectionResult::isValidated);
    if (anyValid) {
      //  mark all as valid
      delegateConnectionResults.forEach(delegateConnectionResult -> delegateConnectionResult.setValidated(true));
    }
    return delegateConnectionResults;
  }

  @Override
  public List<String> getCriteria() {
    Object[] parameters = getParameters();
    final List<String> criteriaList;

    if (!(parameters[0] instanceof HostValidationTaskParameters)) {
      criteriaList = emptyIfNull((List<String>) getParameters()[2]);
    } else {
      HostValidationTaskParameters hostValidationTaskParameters = (HostValidationTaskParameters) getParameters()[0];
      criteriaList = emptyIfNull(hostValidationTaskParameters.getHostNames());
    }

    return criteriaList.stream().map(this ::addPrefix).collect(toList());
  }

  private String addPrefix(String criteria) {
    return BATCH_HOST_VALIDATION + criteria;
  }
}
