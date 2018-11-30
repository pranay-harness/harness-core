package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.sm.ExecutionStatus.SUCCESS;
import static software.wings.utils.Misc.getMessage;

import com.google.inject.Inject;

import io.harness.delegate.task.protocol.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.settings.SettingValue;
import software.wings.settings.validation.ConnectivityValidationAttributes;
import software.wings.settings.validation.ConnectivityValidationDelegateRequest;
import software.wings.settings.validation.ConnectivityValidationDelegateResponse;
import software.wings.settings.validation.SshConnectionConnectivityValidationAttributes;
import software.wings.settings.validation.WinRmConnectivityValidationAttributes;
import software.wings.utils.HostValidationService;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConnectivityValidationTask extends AbstractDelegateRunnableTask {
  @Inject private HostValidationService hostValidationService;

  public ConnectivityValidationTask(String delegateId, DelegateTask delegateTask,
      Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public ConnectivityValidationDelegateResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public ConnectivityValidationDelegateResponse run(Object[] parameters) {
    ConnectivityValidationDelegateRequest request = (ConnectivityValidationDelegateRequest) parameters[0];
    try {
      SettingAttribute settingAttribute = request.getSettingAttribute();
      List<EncryptedDataDetail> encryptedDataDetails = request.getEncryptedDataDetails();
      SettingValue settingValue = settingAttribute.getValue();
      ConnectivityValidationAttributes connectivityValidationAttributes = settingAttribute.getValidationAttributes();
      if (settingValue instanceof HostConnectionAttributes) {
        if (!(connectivityValidationAttributes instanceof SshConnectionConnectivityValidationAttributes)) {
          throw new InvalidRequestException("Must send Ssh connectivity attributes", USER);
        }
        List<String> hostNames = singletonList(
            ((SshConnectionConnectivityValidationAttributes) connectivityValidationAttributes).getHostName());
        ExecutionCredential credential =
            aSSHExecutionCredential().withExecutionType(SSH).withSshUser("").withSshPassword(new char[0]).build();
        List<HostValidationResponse> response =
            hostValidationService.validateHost(hostNames, settingAttribute, encryptedDataDetails, credential);
        if (isEmpty(response)) {
          throw new InvalidRequestException("Did not get hosts validated for SSH", USER);
        }
        return ConnectivityValidationDelegateResponse.builder()
            .executionStatus(SUCCESS)
            .valid(SUCCESS.name().equals(response.get(0).getStatus()))
            .errorMessage(response.get(0).getErrorDescription())
            .build();
      } else if (settingValue instanceof WinRmConnectionAttributes) {
        if (!(connectivityValidationAttributes instanceof WinRmConnectivityValidationAttributes)) {
          throw new InvalidRequestException("Must send Win Rm connectivity attributes", USER);
        }
        List<String> hostNames =
            singletonList(((WinRmConnectivityValidationAttributes) connectivityValidationAttributes).getHostName());
        List<HostValidationResponse> response =
            hostValidationService.validateHost(hostNames, settingAttribute, encryptedDataDetails, null);
        if (isEmpty(response)) {
          throw new InvalidRequestException("Did not get hosts validated for SSH", USER);
        }
        return ConnectivityValidationDelegateResponse.builder()
            .executionStatus(SUCCESS)
            .valid(SUCCESS.name().equals(response.get(0).getStatus()))
            .errorMessage(response.get(0).getErrorDescription())
            .build();

      } else {
        throw new InvalidRequestException(
            format("Connectivity validation not supported for: [%s]", settingValue.getClass().getName()), USER);
      }
    } catch (WingsException exception) {
      throw exception;
    } catch (Exception ex) {
      throw new InvalidRequestException(getMessage(ex), USER);
    }
  }
}