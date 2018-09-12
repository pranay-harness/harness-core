package software.wings.delegatetasks.aws;

import static io.harness.exception.WingsException.ExecutionContext.DELEGATE;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.impl.aws.model.AwsIamListInstanceRolesResponse;
import software.wings.service.impl.aws.model.AwsIamListRolesResponse;
import software.wings.service.impl.aws.model.AwsIamRequest;
import software.wings.service.impl.aws.model.AwsIamRequest.AwsIamRequestType;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsIamHelperServiceDelegate;
import software.wings.waitnotify.NotifyResponseData;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AwsIamTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(AwsEcrTask.class);
  @Inject private AwsIamHelperServiceDelegate iAmServiceDelegate;

  public AwsIamTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    AwsIamRequest request = (AwsIamRequest) parameters[0];
    try {
      AwsIamRequestType requestType = request.getRequestType();
      switch (requestType) {
        case LIST_IAM_ROLES: {
          Map<String, String> iAmRoles =
              iAmServiceDelegate.listIAMRoles(request.getAwsConfig(), request.getEncryptionDetails());
          return AwsIamListRolesResponse.builder().roles(iAmRoles).executionStatus(SUCCESS).build();
        }
        case LIST_IAM_INSTANCE_ROLES: {
          List<String> instanceIamRoles =
              iAmServiceDelegate.listIamInstanceRoles(request.getAwsConfig(), request.getEncryptionDetails());
          return AwsIamListInstanceRolesResponse.builder()
              .instanceRoles(instanceIamRoles)
              .executionStatus(SUCCESS)
              .build();
        }
        default: {
          throw new InvalidRequestException("Invalid request type [" + requestType + "]", WingsException.USER);
        }
      }
    } catch (WingsException exception) {
      WingsExceptionMapper.logProcessedMessages(exception, DELEGATE, logger);
      throw exception;
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage(), WingsException.USER);
    }
  }
}