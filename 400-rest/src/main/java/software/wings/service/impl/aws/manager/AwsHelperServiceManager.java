package software.wings.service.impl.aws.manager;

import static io.harness.exception.ExceptionUtils.getMessage;
import static io.harness.exception.WingsException.USER;

import static java.lang.String.format;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.exception.InvalidRequestException;

import software.wings.service.impl.aws.model.AwsResponse;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class AwsHelperServiceManager {
  void validateDelegateSuccessForSyncTask(DelegateResponseData notifyResponseData) {
    if (notifyResponseData instanceof ErrorNotifyResponseData) {
      throw new InvalidRequestException(((ErrorNotifyResponseData) notifyResponseData).getErrorMessage(), USER);
    } else if (notifyResponseData instanceof RemoteMethodReturnValueData) {
      throw new InvalidRequestException(
          getMessage(((RemoteMethodReturnValueData) notifyResponseData).getException()), USER);
    } else if (!(notifyResponseData instanceof AwsResponse)) {
      throw new InvalidRequestException(
          format("Unknown response from delegate: [%s]", notifyResponseData.getClass().getSimpleName()), USER);
    }
  }
}
