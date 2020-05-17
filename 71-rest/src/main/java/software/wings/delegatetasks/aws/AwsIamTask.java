package software.wings.delegatetasks.aws;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.exception.WingsException.ExecutionContext.DELEGATE;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;
import software.wings.service.impl.aws.model.AwsIamListInstanceRolesResponse;
import software.wings.service.impl.aws.model.AwsIamListRolesResponse;
import software.wings.service.impl.aws.model.AwsIamRequest;
import software.wings.service.impl.aws.model.AwsIamRequest.AwsIamRequestType;
import software.wings.service.impl.aws.model.AwsResponse;
import software.wings.service.intfc.aws.delegate.AwsIamHelperServiceDelegate;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Slf4j
public class AwsIamTask extends AbstractDelegateRunnableTask {
  @Inject private AwsIamHelperServiceDelegate iAmServiceDelegate;

  public AwsIamTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, Supplier<Boolean> preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public AwsResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public AwsResponse run(TaskParameters parameters) {
    AwsIamRequest request = (AwsIamRequest) parameters;
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
      ExceptionLogger.logProcessedMessages(exception, DELEGATE, logger);
      throw exception;
    } catch (Exception ex) {
      throw new InvalidRequestException(ex.getMessage(), WingsException.USER);
    }
  }
}