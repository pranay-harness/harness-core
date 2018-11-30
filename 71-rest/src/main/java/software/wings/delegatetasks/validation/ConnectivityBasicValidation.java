package software.wings.delegatetasks.validation;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.SECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.settings.SettingValue;
import software.wings.settings.validation.ConnectivityValidationDelegateRequest;
import software.wings.settings.validation.SshConnectionConnectivityValidationAttributes;
import software.wings.settings.validation.WinRmConnectivityValidationAttributes;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public class ConnectivityBasicValidation extends AbstractDelegateValidateTask {
  private static final int SOCKET_TIMEOUT = (int) SECONDS.toMillis(15);
  private static final Logger logger = LoggerFactory.getLogger(ConnectivityBasicValidation.class);

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
    int port = 22;
    if (settingValue instanceof WinRmConnectionAttributes) {
      port = ((WinRmConnectionAttributes) settingValue).getPort();
    }
    return getSocketConnectivity(hostName, port, criteria);
  }

  private List<DelegateConnectionResult> getSshValidationResult(ConnectivityValidationDelegateRequest request) {
    SshConnectionConnectivityValidationAttributes validationAttributes =
        (SshConnectionConnectivityValidationAttributes) request.getSettingAttribute().getValidationAttributes();
    String hostName = validationAttributes.getHostName();
    String criteria = getSshCriteria(request).get(0);
    SettingValue settingValue = request.getSettingAttribute().getValue();
    int port = 22;
    if (settingValue instanceof HostConnectionAttributes) {
      port = ((HostConnectionAttributes) settingValue).getSshPort();
    }
    return getSocketConnectivity(hostName, port, criteria);
  }

  private List<DelegateConnectionResult> getSocketConnectivity(String hostName, int port, String criteria) {
    boolean valid = false;
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(hostName, port), SOCKET_TIMEOUT);
      socket.close();
      valid = true;
    } catch (Exception ex) {
      logger.error(format("Exception: [%s] while validating basic socket connectivity", ex.getMessage()), ex);
      valid = false;
    }
    return singletonList(DelegateConnectionResult.builder().criteria(criteria).validated(valid).build());
  }

  private List<String> getWinRmCriteria(ConnectivityValidationDelegateRequest request) {
    WinRmConnectivityValidationAttributes validationAttributes =
        (WinRmConnectivityValidationAttributes) request.getSettingAttribute().getValidationAttributes();
    return singletonList(format("Basic socket connectivity: %s", validationAttributes.getHostName()));
  }

  private List<String> getSshCriteria(ConnectivityValidationDelegateRequest request) {
    SshConnectionConnectivityValidationAttributes validationAttributes =
        (SshConnectionConnectivityValidationAttributes) request.getSettingAttribute().getValidationAttributes();
    return singletonList(format("Basic socket connectivity: %s", validationAttributes.getHostName()));
  }
}