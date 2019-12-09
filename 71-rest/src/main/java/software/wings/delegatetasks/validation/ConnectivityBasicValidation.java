package software.wings.delegatetasks.validation;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.helpers.ext.external.comm.handlers.EmailHandler;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.settings.SettingValue;
import software.wings.settings.validation.ConnectivityValidationDelegateRequest;
import software.wings.settings.validation.SshConnectionConnectivityValidationAttributes;
import software.wings.settings.validation.WinRmConnectivityValidationAttributes;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;
@Slf4j
public class ConnectivityBasicValidation extends AbstractDelegateValidateTask {
  @Inject EmailHandler emailHandler;
  private static final int SOCKET_TIMEOUT = (int) SECONDS.toMillis(15);

  private static final String SLACK_API_CRITERIA = "https://slack.com/api/api.test";

  public ConnectivityBasicValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    ConnectivityValidationDelegateRequest request = (ConnectivityValidationDelegateRequest) getParameters()[0];
    SettingValue settingValue = request.getSettingAttribute().getValue();
    if (settingValue instanceof HostConnectionAttributes) {
      return getSshValidationResult(request);
    } else if (settingValue instanceof WinRmConnectionAttributes) {
      return getWinRmValidationResult(request);
    } else if (settingValue instanceof SmtpConfig) {
      return getSmtpValidationResult(request);
    } else {
      // Should never happen
      return singletonList(DelegateConnectionResult.builder().criteria("").validated(false).build());
    }
  }

  @Override
  public List<String> getCriteria() {
    ConnectivityValidationDelegateRequest request = (ConnectivityValidationDelegateRequest) getParameters()[0];
    SettingValue settingValue = request.getSettingAttribute().getValue();
    if (settingValue instanceof HostConnectionAttributes) {
      return getSshCriteria(request);
    } else if (settingValue instanceof WinRmConnectionAttributes) {
      return getWinRmCriteria(request);
    } else if (settingValue instanceof SmtpConfig) {
      return getSmtpCriteria(request);
    } else {
      // Should never happen
      return singletonList("");
    }
  }

  private List<DelegateConnectionResult> getWinRmValidationResult(ConnectivityValidationDelegateRequest request) {
    WinRmConnectivityValidationAttributes validationAttributes =
        (WinRmConnectivityValidationAttributes) request.getSettingAttribute().getValidationAttributes();
    String hostName = validationAttributes.getHostName();
    String criteria = getWinRmCriteria(request).get(0);
    SettingValue settingValue = request.getSettingAttribute().getValue();
    int port = ((WinRmConnectionAttributes) settingValue).getPort();
    return getSocketConnectivity(hostName, port, criteria);
  }

  private List<DelegateConnectionResult> getSshValidationResult(ConnectivityValidationDelegateRequest request) {
    SshConnectionConnectivityValidationAttributes validationAttributes =
        (SshConnectionConnectivityValidationAttributes) request.getSettingAttribute().getValidationAttributes();
    String hostName = validationAttributes.getHostName();
    String criteria = getSshCriteria(request).get(0);
    SettingValue settingValue = request.getSettingAttribute().getValue();
    int port = ((HostConnectionAttributes) settingValue).getSshPort();
    return getSocketConnectivity(hostName, port, criteria);
  }

  private List<DelegateConnectionResult> getSmtpValidationResult(ConnectivityValidationDelegateRequest request) {
    String criteria = getSmtpCriteria(request).get(0);
    SmtpConfig smtpConfig = (SmtpConfig) request.getSettingAttribute().getValue();
    List<EncryptedDataDetail> encryptedDataDetails = request.getEncryptedDataDetails();
    return singletonList(DelegateConnectionResult.builder()
                             .criteria(criteria)
                             .validated(emailHandler.validateDelegateConnection(smtpConfig, encryptedDataDetails))
                             .build());
  }

  private List<DelegateConnectionResult> getSocketConnectivity(String hostName, int port, String criteria) {
    boolean valid = false;
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(hostName, port), SOCKET_TIMEOUT);
      socket.close();
      valid = true;
    } catch (Exception ex) {
      logger.error("Exception: [{}] while validating basic socket connectivity", ex.getMessage(), ex);
      valid = false;
    }
    return singletonList(DelegateConnectionResult.builder().criteria(criteria).validated(valid).build());
  }

  private List<String> getSshCriteria(ConnectivityValidationDelegateRequest request) {
    SshConnectionConnectivityValidationAttributes validationAttributes =
        (SshConnectionConnectivityValidationAttributes) request.getSettingAttribute().getValidationAttributes();
    return singletonList(format("Basic socket connectivity: %s", validationAttributes.getHostName()));
  }

  private List<String> getWinRmCriteria(ConnectivityValidationDelegateRequest request) {
    WinRmConnectivityValidationAttributes validationAttributes =
        (WinRmConnectivityValidationAttributes) request.getSettingAttribute().getValidationAttributes();
    return singletonList(format("Basic socket connectivity: %s", validationAttributes.getHostName()));
  }

  private List<String> getSmtpCriteria(ConnectivityValidationDelegateRequest request) {
    SmtpConfig smtpConfig = (SmtpConfig) request.getSettingAttribute().getValue();
    return singletonList(format("%s:%s", smtpConfig.getHost(), smtpConfig.getPort()));
  }
}
