package software.wings.delegatetasks;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static software.wings.beans.ExecutionCredential.ExecutionType.SSH;
import static software.wings.beans.SSHExecutionCredential.Builder.aSSHExecutionCredential;
import static software.wings.settings.validation.SmtpConnectivityValidationAttributes.DEFAULT_TEXT;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.HostValidationResponse;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.mail.Mailer;
import software.wings.helpers.ext.mail.SmtpConfig;
import software.wings.settings.SettingValue;
import software.wings.settings.validation.ConnectivityValidationAttributes;
import software.wings.settings.validation.ConnectivityValidationDelegateRequest;
import software.wings.settings.validation.ConnectivityValidationDelegateResponse;
import software.wings.settings.validation.SmtpConnectivityValidationAttributes;
import software.wings.settings.validation.SshConnectionConnectivityValidationAttributes;
import software.wings.settings.validation.WinRmConnectivityValidationAttributes;
import software.wings.utils.HostValidationService;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ConnectivityValidationTask extends AbstractDelegateRunnableTask {
  @Inject private HostValidationService hostValidationService;
  @Inject private Mailer mailer;

  public ConnectivityValidationTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
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

      } else if (settingValue instanceof SmtpConfig) {
        if (!(connectivityValidationAttributes instanceof SmtpConnectivityValidationAttributes)) {
          throw new InvalidRequestException("Must send Smtp connectivity attributes", USER);
        }
        SmtpConnectivityValidationAttributes validationAttributes =
            (SmtpConnectivityValidationAttributes) connectivityValidationAttributes;
        EmailData emailData =
            EmailData.builder()
                .to(singletonList(validationAttributes.getTo()))
                .subject(
                    isNotEmpty(validationAttributes.getSubject()) ? validationAttributes.getSubject() : DEFAULT_TEXT)
                .body(isNotEmpty(validationAttributes.getBody()) ? validationAttributes.getBody() : DEFAULT_TEXT)
                .build();
        boolean valid = false;
        String errorMessage = "";
        try {
          mailer.send((SmtpConfig) settingValue, encryptedDataDetails, emailData);
          valid = true;
        } catch (Exception ex) {
          errorMessage = ExceptionUtils.getMessage(ex);
        }
        return ConnectivityValidationDelegateResponse.builder()
            .executionStatus(SUCCESS)
            .errorMessage(errorMessage)
            .valid(valid)
            .build();
      } else {
        throw new InvalidRequestException(
            format("Connectivity validation not supported for: [%s]", settingValue.getClass().getName()), USER);
      }
    } catch (WingsException exception) {
      throw exception;
    } catch (Exception ex) {
      throw new InvalidRequestException(ExceptionUtils.getMessage(ex), USER);
    }
  }
}